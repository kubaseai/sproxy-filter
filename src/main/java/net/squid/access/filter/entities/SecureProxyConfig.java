package net.squid.access.filter.entities;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecureProxyConfig {
	
	public static class NetworkSource {
		private String ip;
		@JsonProperty("system-id")
		private String systemId;
		@JsonProperty("app-id")
	    private String appId;
		@JsonProperty("user-id")
	    private String userId;
		@JsonProperty("user-token")
	    private String userToken;
	    
		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public String getSystemId() {
			return systemId;
		}
		public void setSystemId(String systemId) {
			this.systemId = systemId;
		}
		public String getAppId() {
			return appId;
		}
		public void setAppId(String appId) {
			this.appId = appId;
		}
		public String getUserId() {
			return userId;
		}
		public void setUserId(String userId) {
			this.userId = userId;
		}
		public String getUserToken() {
			return userToken;
		}
		public void setUserToken(String userToken) {
			this.userToken = userToken;
		}
		@Override
		public String toString() {
			return String.format("NetworkSource [ip=%s, systemId=%s, appId=%s, userId=%s]", ip, systemId, appId,
				userId);
		}
		public String getUserCredentials() {
			return userId+"="+userToken;
		}
	}
	
	public enum Method {
		GET,
		POST,
		PUT,
		PATCH,
		DELETE,
		HEAD,
		OPTIONS,
		CONNECT
	}
	
	public static class NetworkDestination {
		private String id;
		@JsonProperty("system-id")
		private String systemId;
		private URI[] uris;
		private String[] methods;
		@JsonProperty("content-sent")
		private String[] contentSent;
		@JsonProperty("content-received")
		private String[] contentReceived;
		@JsonProperty("ssl-inspection")
		private boolean sslInspection = true;
		@JsonProperty("ssl-inspection-skip-reason")
		private String sslInspectionSkipReason;
		@JsonProperty("av-scan")
		private boolean avScan = true;
		@JsonProperty("av-scan-skip-reason")
		private String avScanSkipReason;
		
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getSystemId() {
			return systemId;
		}
		public void setSystemId(String systemId) {
			this.systemId = systemId;
		}
		public List<URI> getUris() {
			return Arrays.asList(uris!=null ? uris : new URI[0]);
		}
		public void setUris(URI[] uris) {
			this.uris = uris;
		}
		public List<String> getMethods() {
			return Arrays.asList(methods!=null ? methods : new String[0]);
		}
		public void setMethods(String methods) {
			String[] mm = methods.split("\\,");
			LinkedList<String> normalizedMethods = new LinkedList<>();
			for (var m : mm) {
				String method = m.trim();
				try {
					Method.valueOf(method);
				}
				finally {}
				normalizedMethods.add(method);
			}
			this.methods = normalizedMethods.toArray(new String[normalizedMethods.size()]);
		}		
		public void setMethods(String[] methods) {
			for (var m : methods) {
				if (Method.valueOf(m)==null) {
					throw new RuntimeException("Unsupported method: "+m);
				}
			}
			this.methods = methods;
		}
		public List<String> getContentSent() {
			return Arrays.asList(contentSent!=null ? contentSent : new String[0]);
		}
		public void setContentSent(String[] contentSent) {
			this.contentSent = contentSent;
		}
		public void setContentSent(String contentSent) {
			String[] ct = contentSent.split("\\,");
			LinkedList<String> normalized = new LinkedList<>();
			for (var s : ct) {
				s = s.trim();
				normalized.add(s);
			}
			this.contentSent = normalized.toArray(new String[normalized.size()]);
		}
		public List<String> getContentReceived() {
			return Arrays.asList(contentReceived!=null ? contentReceived : new String[0]);
		}
		public void setContentReceived(String[] contentReceived) {
			this.contentReceived = contentReceived;
		}
		public void setContentReceived(String contentReceived) {
			String[] ct = contentReceived.split("\\,");
			LinkedList<String> normalized = new LinkedList<>();
			for (var s : ct) {
				s = s.trim();
				normalized.add(s);
			}
			this.contentReceived = normalized.toArray(new String[normalized.size()]);
		}
		public boolean isSslInspection() {
			return sslInspection;
		}
		public void setSslInspection(boolean sslInspection) {
			this.sslInspection = sslInspection;
		}
		public String getSslInspectionSkipReason() {
			return sslInspectionSkipReason;
		}
		public void setSslInspectionSkipReason(String sslInspectionSkipReason) {
			this.sslInspectionSkipReason = sslInspectionSkipReason;
		}
		public boolean isAvScan() {
			return avScan;
		}
		public void setAvScan(boolean avScan) {
			this.avScan = avScan;
		}
		public String getAvScanSkipReason() {
			return avScanSkipReason;
		}
		public void setAvScanSkipReason(String avScanSkipReason) {
			this.avScanSkipReason = avScanSkipReason;
		}
		@Override
		public String toString() {
			return String.format(
				"NetworkDestination [id=%s, systemId=%s, uris=%s, methods=%s, sslInspection=%s, sslInspectionSkipReason=%s, avScan=%s, avScanSkipReason=%s]",
				id, systemId, getUris(), getMethods(), sslInspection, sslInspectionSkipReason, avScan, avScanSkipReason);
		}		
	}
	
	@JsonProperty("config-id")
	private String configId;
	@JsonProperty("network-source")
	private NetworkSource[] networkSource;
	@JsonProperty("network-destinations")
	private NetworkDestination[] networkDestinations;
	
	public String getConfigId() {
		return configId;
	}
	public void setConfigId(String configId) {
		this.configId = configId;
	}
	public List<NetworkSource> getNetworkSource() {
		return Arrays.asList(networkSource!=null ? networkSource : new NetworkSource[0]);
	}
	public void setNetworkSource(NetworkSource[] networkSource) {
		this.networkSource = networkSource;
	}
	public List<NetworkDestination> getNetworkDestinations() {
		return Arrays.asList(networkDestinations!=null ? networkDestinations : new NetworkDestination[0]);
	}
	public void setNetworkDestinations(NetworkDestination[] networkDestinations) {
		this.networkDestinations = networkDestinations;
	}
	@Override
	public String toString() {
		return String.format("SecureProxyConfig [configId=%s, networkSource=%s, networkDestinations=%s]", configId,
				getNetworkSource(), getNetworkDestinations());
	}
}
