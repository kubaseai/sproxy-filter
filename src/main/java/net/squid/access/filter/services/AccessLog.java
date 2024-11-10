package net.squid.access.filter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccessLog {
	private static Logger log = LoggerFactory.getLogger(AccessLog.class);

	public void logEntry(String entry) {
		log.info(entry);		
	}
}
