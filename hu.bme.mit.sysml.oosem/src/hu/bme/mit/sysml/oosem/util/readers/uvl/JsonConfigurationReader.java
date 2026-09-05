package hu.bme.mit.sysml.oosem.util.readers.uvl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonConfigurationReader implements ConfigurationReader {

	@Override
	public ConfigurationSelection readSelection(File configFile) throws IOException {
		String content = Files.readString(configFile.toPath());

		List<String> selectedIds = new ArrayList<>();
		List<String> unselectedIds = new ArrayList<>();
		Matcher matcher = ENTRY_PATTERN.matcher(content);
		while (matcher.find()) {
			String id = matcher.group(1).replace("\\\\", "\\");
			boolean selected = Boolean.parseBoolean(matcher.group(2));
			if (selected)
				selectedIds.add(id);
			else
				unselectedIds.add(id);
		}
		return new ConfigurationSelection(selectedIds, unselectedIds);
	}

	private static final Pattern ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(true|false)");
}