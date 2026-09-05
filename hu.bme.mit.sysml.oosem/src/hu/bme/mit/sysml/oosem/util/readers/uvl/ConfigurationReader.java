package hu.bme.mit.sysml.oosem.util.readers.uvl;

import java.io.File;
import java.io.IOException;
 
public interface ConfigurationReader {
 
	ConfigurationSelection readSelection(File configFile) throws IOException;
}
