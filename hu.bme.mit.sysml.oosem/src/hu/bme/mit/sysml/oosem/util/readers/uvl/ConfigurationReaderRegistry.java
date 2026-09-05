package hu.bme.mit.sysml.oosem.util.readers.uvl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConfigurationReaderRegistry {
 
	private static final Map<String, ConfigurationReader> READERS = new HashMap<>();
 
	static {
		READERS.put("json", new JsonConfigurationReader());
	}

	private ConfigurationReaderRegistry() {}
 
	public static Optional<ConfigurationReader> getReader(String extension) {
		return Optional.ofNullable(READERS.get(extension == null ? "" : extension.toLowerCase()));
	}
}
