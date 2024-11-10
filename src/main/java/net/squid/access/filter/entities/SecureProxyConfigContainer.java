package net.squid.access.filter.entities;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecureProxyConfigContainer {	
	@JsonProperty("secure-proxy-config")
	private SecureProxyConfig[] secureProxyConfig;

	public List<SecureProxyConfig> getSecureProxyConfig() {
		return Arrays.asList(secureProxyConfig!=null ? secureProxyConfig : new SecureProxyConfig[0]);
	}

	public void setSecureProxyConfig(SecureProxyConfig[] secureProxyConfig) {
		this.secureProxyConfig = secureProxyConfig;
	}
}
