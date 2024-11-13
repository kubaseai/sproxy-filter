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

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.squid.access.filter.entities.Config;
import net.squid.access.filter.entities.SecureProxyConfig;
import net.squid.access.filter.entities.SecureProxyConfigContainer;

@Service
public class ConfigReader {
	
	private final Config cfg;
	private final ConcurrentHashMap<String,SecureProxyConfig> cfgByIp =
		new ConcurrentHashMap<>();
	private static Logger log = LoggerFactory.getLogger(ConfigReader.class);
	private AtomicBoolean reloadingFlag = new AtomicBoolean(false);
	private volatile boolean initialized = false;
	private volatile long runCount = 0;
		
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

	@Scheduled(fixedRate = 180000)
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
	
	private void _reloadConfiguration() throws InterruptedException {
		log.info("Reload config request started, initialized="+initialized);
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		ExecutorService es = Executors.newWorkStealingPool(cfg.getNumberOfConfigWorkers());
		List<File> listOfConfigFiles = listConfigFiles();
		if (runCount > Integer.MAX_VALUE) {
			runCount = 0;
		}
		final long version = ++runCount;
		
		listOfConfigFiles.stream().forEach( f -> {
			es.submit(() -> {			
				try {
					mapper.readValue(f, SecureProxyConfigContainer.class)
					.getSecureProxyConfig().forEach( c -> {
						c.version = version;
						c.getNetworkSource().forEach( src -> {
							for (String ipOrSubnet : src.getIp()) {
								if (ipOrSubnet.contains("/")) {
									var subnetInfo = new SubnetUtils(ipOrSubnet).getInfo();
									for (String a : subnetInfo.getAllAddresses()) {
										SecureProxyConfig previous = cfgByIp.put(a, c);
										if (previous!=null && previous.version == c.version) {
											log.error("Configuration overlap detected on "+a+". Rebinding to user.\nOld: "+previous+"\nNew: "+c);
											rebindToUser(previous, cfgByIp);
											rebindToUser(c, cfgByIp);
											cfgByIp.remove(a, c);
											break;
										}
									};
									if (subnetInfo.getAddressCountLong()==0) {
										log.error("Configuration contains empty subnet="+ipOrSubnet+": "+c);
									}
								}							
								else {
									SecureProxyConfig previous = cfgByIp.put(ipOrSubnet, c);
									if (previous!=null && previous.version == c.version) {
										log.warn("Configuration overlap detected on "+ipOrSubnet+". Rebinding to user.\nOld: "+previous+"\nNew: "+c);
										rebindToUser(previous, cfgByIp);
										rebindToUser(c, cfgByIp);
										cfgByIp.remove(ipOrSubnet, c);
									}
								}
							}
						});				
					});					
				}
				catch (Exception e) {
					log.error("Cannot process SecureProxyConfig file "+f.getAbsolutePath(), e);
				}
			});
		});
		es.shutdown();
		es.awaitTermination(cfg.getConfigReloadTimeoutSeconds(), TimeUnit.SECONDS);
		int notFinished = es.shutdownNow().size();
		var it = cfgByIp.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			if (e.getValue().version < version) {
				it.remove();
			}			
		}
		log.info("Reload config request "+version+" finished, number of files="+listOfConfigFiles.size()+
			", entries="+cfgByIp.size()+", orphaned tasks="+notFinished);
	}
	
	private void rebindToUser(SecureProxyConfig c, ConcurrentHashMap<String,SecureProxyConfig> cfgByIp) {
		c.getNetworkSource().forEach(src -> {
			for (String ipOrSubnet : src.getIp()) {
				if (ipOrSubnet.contains("/")) {
					var subnetInfo = new SubnetUtils(ipOrSubnet).getInfo();
					for (String a : subnetInfo.getAllAddresses()) {
						SecureProxyConfig previous = cfgByIp.put(a+":"+src.getUserCredentials(), c);
						if (previous!=null && previous.version==c.version) {
							log.error("Configuration overlap detected while rebinding to user.\nOld: "+previous+"\nNew: "+c);
						}
					};
				}
				else {
					SecureProxyConfig previous = cfgByIp.put(ipOrSubnet+":"+src.getUserCredentials(), c);
					if (previous!=null) {
						log.error("Configuration overlap detected while rebinding to user.\nOld: "+previous+"\nNew: "+c);
					}
				}
			}
		});		
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
