package net.squid.access.filter.entities;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sproxy")
public class Config {	
	private String configDir;
	private int numberOfConfigWorkers = 40;
	private int configReloadTimeoutSec = 600;

	public String getConfigDir() {
		return configDir;
	}

	public void setConfigDir(String configDir) {
		this.configDir = configDir;
	}

	public int getNumberOfConfigWorkers() {
		return numberOfConfigWorkers;
	}

	public int getConfigReloadTimeoutSeconds() {
		return configReloadTimeoutSec ;
	}
}