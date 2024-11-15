package net.squid.access.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ResourceUtils;

import net.squid.access.filter.entities.Config;
import net.squid.access.filter.entities.SecureProxyConfig;
import net.squid.access.filter.entities.SecureProxyConfig.NetworkDestination;
import net.squid.access.filter.services.AccessLog;
import net.squid.access.filter.services.ConfigReader;

/** see: https://gist.github.com/Piumal1999/6fc54eeb57d04e8e343fc12777bfcb45 */
@SpringBootApplication
@EnableScheduling
public class FilterApplication implements CommandLineRunner {
	
	private static Logger log = LoggerFactory.getLogger(FilterApplication.class);
	@Autowired
	private ConfigReader reader;
	@Autowired
	private AccessLog accessLog;
	@Autowired
	private Config config;
	private static ConcurrentHashMap<String, Integer> wellKnownPorts = new ConcurrentHashMap<>();
	private static OutputStream out;
	private static AntPathMatcher urlMatcher = new AntPathMatcher();
	
	static {
		wellKnownPorts.put("http", 80);
		wellKnownPorts.put("https", 443);
		wellKnownPorts.put("ftp", 21);
		wellKnownPorts.put("ftps", 990);
		wellKnownPorts.put("ssh", 22);
		wellKnownPorts.put("scp", 22);
		wellKnownPorts.put("sftp", 22);
	}
	
	public static void main(String[] args) throws Exception {		
		if (args.length >= 1 && args[0].contains("config")) {
			System.out.println(retrieveSquidConfig());
			return;
		}
		rewireSystemOut();
		SpringApplication.run(FilterApplication.class, args);
	}	
	
	private static void rewireSystemOut() {
		out = System.out;
		System.setOut(System.err);		
	}

	private final static String ACCEPT_SSL_AV = "OK clt_conn_tag=filtered_SSL_AV\n";
	private final static String ACCEPT_SSL = "OK clt_conn_tag=filtered_SSL\n";
	private final static String ACCEPT_AV = "OK clt_conn_tag=filtered_AV\n";
	private final static String ACCEPT_PASS = "OK clt_conn_tag=filtered_PASS\n";
	private final static String DENY = "OK status=302 url=\"http://example.com\"\n";
	
	private WeakHashMap<String, String> authByPeer = new WeakHashMap<>();
	
	public static String retrieveSquidConfig() throws Exception {
        File file = ResourceUtils.getFile("classpath:sproxy.conf");
        byte[] content = Files.readAllBytes(Path.of(file.toURI()));
        return new String(content);        
    }
	
	@Override
	public void run(String... args) throws Exception {
		log.info("Starting filter app");
		try (InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr)) {
			String line = null;
			while ((line=br.readLine())!=null) {
				log.debug("Line: "+line);
				if (line.trim().length()==0)
					continue;
				String[] tokens = line.split("\\ ");
				int b = 0;
				String transaction = "";
				try {
					Integer.valueOf(tokens[0]);
					transaction = tokens[0]+" ";
					b=1;
				}
				catch (Exception e) {}
				if (tokens.length < 7) {
					log.error("Wrong squid configuration. It must be: url_rewrite_extras \"%>a:%>p %{X-Forwarded-For}>h %{Proxy-Authorization}>h %>rm %{Content-Type}>h %{Accept}>h\". Line was: "+line);
					System.exit(2);
				}
				var url = tokens[b];
				var ipPort = tokens[b+1];
				var shadowedIp = tokens[b+2];
				var m = tokens[b+4];
				var authText = tokens[b+3].length() > 8 ? unbase64(tokens[b+3].substring(8)) : "";
				var user = getBasicUser(authText);				
				var secret = hash(authText);
				if (!url.startsWith("http")) { // domain, not URL
					authByPeer.put(ipPort, user+"="+secret);
				}
				else {
					var auth = authByPeer.get(ipPort);
					if (auth!=null && auth.contains("=")) {
						String[] us = auth.split("\\=");
						user = us[0];
						secret = us[1];
					}
				}
				var ip = (shadowedIp.length() > 0 && !"-".equals(shadowedIp) && config.isTrustXForwardedFor()) ?
					shadowedIp : ipPort.split("\\:")[0];
				Map<String,SecureProxyConfig> configMap = reader.getConfigByIp();
				SecureProxyConfig cfg = configMap.get(ip+":"+user+"="+secret);
				if (cfg==null) {
					cfg = configMap.get(ip);
					if (cfg==null) {
						cfg = configMap.get("any");
					}
				}
				var ctOut = tokens[b+5];
				var ctIn = tokens[b+6];
				String logEntry = "src="+ipPort+"/"+shadowedIp+", user="+user+", verb="+m+", dst="+url+", ct_out="+ctOut+", ctIn="+ctIn;
				String status = transaction+filter(cfg, url, m, ctOut, ctIn, logEntry);
				out.write(status.getBytes());
				out.flush();				
			}
		}
		catch (Exception e) {
			log.error("!!! Runtime filter exiting due exception !!!", e);
			System.exit(1);
		}
	}
	
	private String getHostFromUri(URI uri) {
		String[] tab = uri.toString().split("\\/");
		for (int i=1; i < tab.length; i++) {
			if (tab[i].length()>0) {
				return tab[i];
			}
		}
		return null;
	}
	
	private String filter(SecureProxyConfig cfg, String url, String m, String ctSend, String ctRecv,
		String logEntry) throws MalformedURLException
	{
		if (cfg==null) {
			accessLog.logEntry("DENY :: "+logEntry+" // reason(s): no rule by ip");
			return DENY;
		}
		StringBuilder why = new StringBuilder();
		for (NetworkDestination dst : cfg.getNetworkDestinations()) {
			for (URI uri : dst.getUris()) {
				if (!url.startsWith("http")) { // it's not full URL but host:port only
					String host = getHostFromUri(uri);
					if (host.indexOf(":")==-1) {
						host += ":"+getPortForProtocol(uri.getScheme());
					}
					if (urlMatcher.match("http://"+host, "http://"+url)) {
						return accept(dst, logEntry);
					}
					why.append("rule by host "+host+" not equal to actual "+url+"; ");
				}
				String urlPattern = uri.toURL().toString();
				if (urlMatcher.match(urlPattern,url) && dst.getMethods().contains(m)) {
					if (contentAllowed(dst, ctSend, ctRecv))
						return accept(dst, logEntry);
					why.append("URL and methods OK, but content type not OK; ");
				}
				why.append(urlPattern+" not equals "+url+" for allowed methods "+dst.getMethods()+" and given "+m+"; ");
			}
		}
		accessLog.logEntry("DENY :: "+logEntry+" // reason(s): "+why);
		return DENY;
	}

	private int getPortForProtocol(String scheme) {
		return wellKnownPorts.getOrDefault(scheme, -1);
	}

	private boolean contentAllowed(NetworkDestination dst, String ctSend, String ctRecv) {
		List<String> ruleCtReceived = dst.getContentReceived();
		List<String> ruleCtSent = dst.getContentSent();
		if (ctSend==null) ctSend = "-";
		if (ctRecv==null) ctRecv = "-";
		if (ruleCtReceived.isEmpty() && ruleCtSent.isEmpty()) {
			return true;
		}
		if (!ruleCtSent.isEmpty() && !ruleCtSent.contains(ctSend)) {
			return false;
		}
		if (!ruleCtReceived.isEmpty() && !ruleCtReceived.contains(ctRecv)) {
			return false;
		}
		return true;
	}

	private String accept(NetworkDestination dst, String logEntry) {
		if (dst.isSslInspection() && dst.isAvScan()) {
			accessLog.logEntry("ACCEPT_WITH_SSL_AV("+dst.getId()+") :: "+logEntry);
			return ACCEPT_SSL_AV;
		}
		else if (dst.isSslInspection() && !dst.isAvScan()) {
			accessLog.logEntry("ACCEPT_WITH_SSL_AV("+dst.getId()+") :: "+logEntry);
			return ACCEPT_SSL;
		}
		else if (dst.isAvScan()) {
			accessLog.logEntry("ACCEPT_WITH_AV("+dst.getId()+") :: "+logEntry);
			return ACCEPT_AV;
		}
		accessLog.logEntry("ACCEPT_PASSTRU("+dst.getId()+") :: "+logEntry);
		return ACCEPT_PASS;
	}	

	private String hash(String authText) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(authText.getBytes("UTF-8"));
		return bytesToHex(hash);
	}
	
	private static String bytesToHex(byte[] hash) {
	    StringBuilder hexString = new StringBuilder(2 * hash.length);
	    for (int i = 0; i < hash.length; i++) {
	        String hex = Integer.toHexString(0xff & hash[i]);
	        if(hex.length() == 1) {
	            hexString.append('0');
	        }
	        hexString.append(hex);
	    }
	    return hexString.toString();
	}

	private String getBasicUser(String authText) {
		int sepPos = authText.indexOf(":");
		return sepPos > 0 ? authText.substring(0, sepPos) : "";
	}	
	private String unbase64(String auth) {
		return new String(Base64.getDecoder().decode(auth));
	}
}
