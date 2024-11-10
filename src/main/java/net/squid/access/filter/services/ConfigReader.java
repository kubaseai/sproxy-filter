package net.squid.access.filter.services;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
	private final AtomicReference<Map<String,SecureProxyConfig>> cfgByIpRef =
		new AtomicReference<>();
	private static Logger log = LoggerFactory.getLogger(ConfigReader.class);
	private AtomicBoolean reloadFlag = new AtomicBoolean(false);
	private volatile boolean initialized = false;
	
	public ConfigReader(Config cfg) {
		this.cfg = cfg;
	}
	
	private List<File> listConfigFiles() {
		File cfgDir = new File(cfg.getConfigDir());
		if (!cfgDir.exists()) {
			return Collections.emptyList();
		}
		return List.of(cfgDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".yaml");
			}
		}));
	}

	@Scheduled(fixedRate = 60000)
	public void reloadConfiguration() {
		if (reloadFlag.compareAndExchange(false, true)==false) {
			long start = System.currentTimeMillis();
			try {
				_reloadConfiguration();
				long end = System.currentTimeMillis();
				int size = Optional.ofNullable(cfgByIpRef.get()).orElse(new HashMap<>()).size();
				initialized = true;
				log.info("Configuration reloaded in "+(end-start)+" ms, count="+size);
			} 
			catch (InterruptedException e) {
				log.error("Timeout while reloading configuration", e);
			}
			finally {
				reloadFlag.set(false);
			}
		}
		else {
			log.warn("Configuration reloading flag still active, skipping run");
		}
	}
	
	private void _reloadConfiguration() throws InterruptedException {
		log.info("Reload config request started");
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		ConcurrentHashMap<String,SecureProxyConfig> cfgByIp = new ConcurrentHashMap<>();
		ExecutorService es = Executors.newFixedThreadPool(cfg.getNumberOfConfigWorkers());
		listConfigFiles().stream().forEach( f -> {
			es.submit(() -> {			
				try {
					mapper.readValue(f, SecureProxyConfigContainer.class)
					.getSecureProxyConfig().forEach( c -> {
						c.getNetworkSource().forEach( src -> {
							if (src.getIp().contains("/")) {
								SubnetUtils subnetUtils = new SubnetUtils(src.getIp());
								for (var a : Arrays.asList(subnetUtils.getInfo().getAllAddresses())) {
									SecureProxyConfig previous = cfgByIp.put(a, c);
									if (previous!=null) {
										log.warn("Configuration overlap detected. Rebinding to user.\nOld: "+previous+"\nNew: "+c);
										rebindToUser(previous, cfgByIp);
										rebindToUser(c, cfgByIp);
										cfgByIp.remove(a, c);
										break;
									}
								};
							}
							else {
								SecureProxyConfig previous = cfgByIp.put(src.getIp(), c);
								if (previous!=null) {
									log.warn("Configuration overlap detected. Rebinding to user.\nOld: "+previous+"\nNew: "+c);
									rebindToUser(previous, cfgByIp);
									rebindToUser(c, cfgByIp);
									cfgByIp.remove(src.getIp(), c);
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
		es.shutdownNow();
		cfgByIpRef.set(cfgByIp);
		log.info("Reload config request finished");
	}
	
	private void rebindToUser(SecureProxyConfig c, ConcurrentHashMap<String,SecureProxyConfig> cfgByIp) {
		c.getNetworkSource().forEach(src -> {
			if (src.getIp().contains("/")) {
				SubnetUtils subnetUtils = new SubnetUtils(src.getIp());
				for (var a : Arrays.asList(subnetUtils.getInfo().getAllAddresses())) {
					SecureProxyConfig previous = cfgByIp.put(a+":"+src.getUserCredentials(), c);
					if (previous!=null) {
						log.error("Configuration overlap detected. Rebinding to user.\nOld: "+previous+"\nNew: "+c);
					}
				};
			}
			else {
				SecureProxyConfig previous = cfgByIp.put(src.getIp()+":"+src.getUserCredentials(), c);
				if (previous!=null) {
					log.error("Configuration overlap detected. Rebinding to user.\nOld: "+previous+"\nNew: "+c);
				}
			}
		});		
	}

	public Map<String,SecureProxyConfig> getConfigByIp() {
		if (!initialized) {
			reloadConfiguration();
		}
		return cfgByIpRef.get();
	}
}
