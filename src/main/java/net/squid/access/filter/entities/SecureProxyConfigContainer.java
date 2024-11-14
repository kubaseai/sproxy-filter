package net.squid.access.filter.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecureProxyConfigContainer {	
	@JsonProperty("secure-proxy-config")
	private SecureProxyConfig[] secureProxyConfig;

	public SecureProxyConfig[] getSecureProxyConfig() {
		return secureProxyConfig!=null ? secureProxyConfig : new SecureProxyConfig[0];
	}

	public void setSecureProxyConfig(SecureProxyConfig[] secureProxyConfig) {
		this.secureProxyConfig = secureProxyConfig;
	}
}
