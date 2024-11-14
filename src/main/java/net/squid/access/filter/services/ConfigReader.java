package net.squid.access.filter.services;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.squid.access.filter.entities.Config;
import net.squid.access.filter.entities.SecureProxyConfig;
import net.squid.access.filter.entities.SecureProxyConfig.NetworkSource;
import net.squid.access.filter.entities.SecureProxyConfigContainer;
import net.squid.access.filter.entities.Subnet;

@Service
public class ConfigReader {
	
	private final Config cfg;
	private final ConcurrentHashMap<String,SecureProxyConfig> cfgByIp =
		new ConcurrentHashMap<>();
	private static Logger log = LoggerFactory.getLogger(ConfigReader.class);
	private AtomicBoolean reloadingFlag = new AtomicBoolean(false);
	private volatile boolean initialized = false;
	private AtomicLong runCount = new AtomicLong(0);
	private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		
	public ConfigReader(Config cfg) {
		this.cfg = cfg;
	}
	
	private List<File> listConfigFiles() {
		File cfgDir = new File(cfg.getConfigDir());
		if (!cfgDir.exists()) {
			if (!initialized) {
				log.error("Sproxy config dir with yaml files doesn't exist" );
			}
			return Collections.emptyList();
		}
		return List.of(cfgDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".yaml") || name.endsWith(".yml");
			}
		}));
	}

	@Scheduled(timeUnit = TimeUnit.SECONDS, fixedRate = 120)
	public void reloadConfiguration() {
		if (reloadingFlag.compareAndExchange(false, true)==false) {
			long start = System.currentTimeMillis();
			try {
				_reloadConfiguration();
				long end = System.currentTimeMillis();
				int size = cfgByIp.size();
				initialized = true;
				log.info("Configuration reloaded on schedule in "+(end-start)+" ms, count="+size);
			} 
			catch (InterruptedException e) {
				log.error("Timeout while reloading configuration", e);
			}
			finally {
				reloadingFlag.set(false);
			}
		}
		else {
			log.warn("Configuration reloading flag still active, skipping run");
		}
	}
	
	private synchronized void _reloadConfiguration() throws InterruptedException {
		log.info("Reload config request started, initialized="+initialized);		
		final ExecutorService es = Executors.newWorkStealingPool(cfg.getNumberOfConfigWorkers());
		List<File> listOfConfigFiles = listConfigFiles();
		runCount.compareAndSet(Integer.MAX_VALUE, 0);
		final long version = runCount.incrementAndGet();
				
		for (File f : listOfConfigFiles) {
			es.submit(() -> {			
				try {
					for (var c : mapper.readValue(f, SecureProxyConfigContainer.class)
						.getSecureProxyConfig())
					{
						c.version = version;
						for (NetworkSource src : c.getNetworkSource()) {
							log.info("Network source in "+f.getName()+" with IPs: "+src.getIp());
							for (String ipOrSubnet : src.getIp()) {
								if (ipOrSubnet.contains("/")) {
									Subnet subnet = new Subnet(ipOrSubnet);
									if (subnet.getMask() <= 8 || !subnet.isValid()) {
										log.error("Invalid subnet: "+ipOrSubnet+", err="+subnet.getErrorMessage());
										continue;
									}
									while (true) {
										String a = subnet.next();
										if (a==null) {
											break;
										}
										SecureProxyConfig previous = cfgByIp.put(a, c);
										if (previous!=null && previous.version == c.version) {
											log.error("Configuration overlap detected on "+a+". Rebinding to user.\nOld: "+previous+"\nNew: "+c);
											rebindToUser(previous, cfgByIp);
											rebindToUser(c, cfgByIp);
											cfgByIp.remove(a, c);
											break;
										}
									}								
								}							
								else {
									log.info("CFG: Single ip "+ipOrSubnet);
									SecureProxyConfig previous = cfgByIp.put(ipOrSubnet, c);
									if (previous!=null && previous.version == c.version) {
										log.error("Configuration overlap detected on "+ipOrSubnet+". Rebinding to user.\nOld: "+previous+"\nNew: "+c);
										rebindToUser(previous, cfgByIp);
										rebindToUser(c, cfgByIp);
										cfgByIp.remove(ipOrSubnet, c);
									}
								}
							}
						}				
					};				
				}
				catch (Throwable e) {
					log.error("Cannot process SecureProxyConfig file "+f.getAbsolutePath(), e);
				}
			});
		};
		es.shutdown();
		es.awaitTermination(cfg.getConfigReloadTimeoutSeconds(), TimeUnit.SECONDS);
		int notFinished = es.shutdownNow().size();
		var it = cfgByIp.entrySet().iterator();
		int cleared = 0;
		while (it.hasNext()) {
			var e = it.next();
			if (e.getValue().version < version) {
				it.remove();
				cleared++;
				if (cleared==1) {
					log.info("Clearing old config version "+e.getValue().version+" vs "+version);
				}
			}			
		}
		log.info("Reload config request "+version+" finished, number of files="+listOfConfigFiles.size()+
			", entries="+cfgByIp.size()+", cleared="+cleared+", orphaned tasks="+notFinished);
	}
	
	private void rebindToUser(SecureProxyConfig c, ConcurrentHashMap<String,SecureProxyConfig> cfgByIp) {
		for (NetworkSource src : c.getNetworkSource()) {
			for (String ipOrSubnet : src.getIp()) {
				if (ipOrSubnet.contains("/")) {
					Subnet subnet = new Subnet(ipOrSubnet);
					if (subnet.getMask() <= 8 || !subnet.isValid()) {
						log.error("Invalid subnet: "+ipOrSubnet+", err="+subnet.getErrorMessage());
						continue;
					}
					while (true) {
						String a = subnet.next();
						if (a==null) {
							break;
						}
						SecureProxyConfig previous = cfgByIp.put(a+":"+src.getUserCredentials(), c);
						if (previous!=null && previous.version==c.version) {
							log.error("Configuration overlap detected while rebinding to user.\nOld: "+previous+"\nNew: "+c);
						}
					}
				}
				else {
					SecureProxyConfig previous = cfgByIp.put(ipOrSubnet+":"+src.getUserCredentials(), c);
					if (previous!=null) {
						log.error("Configuration overlap detected while rebinding to user.\nOld: "+previous+"\nNew: "+c);
					}
				}
			}
		}	
	}

	public Map<String,SecureProxyConfig> getConfigByIp() {
		if (!initialized) {
			reloadConfiguration();
			for (int i=0; i < 10; i++) {
				if (initialized) {
					break;
				}
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e) {}
			}
		}
		return cfgByIp;
	}
}
