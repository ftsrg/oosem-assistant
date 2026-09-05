package hu.bme.mit.sysml.oosem.wizards;

import java.io.File;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;

import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData;
import hu.bme.mit.sysml.oosem.generators.OOSEMBlockGenerator;
import hu.bme.mit.sysml.oosem.generators.SysMLFileWriter;
import hu.bme.mit.sysml.oosem.generators.UVLconfigProcessor;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMBlock;
import hu.bme.mit.sysml.oosem.model.project.interfaces.OOSEMProject;
import hu.bme.mit.sysml.oosem.util.readers.uvl.ConfigurationReader;
import hu.bme.mit.sysml.oosem.util.readers.uvl.ConfigurationReaderRegistry;
import hu.bme.mit.sysml.oosem.util.readers.uvl.ConfigurationSelection;
import hu.bme.mit.sysml.oosem.wizards.pages.UVLbasedIntegrationPage;

public class UVLbasedIntegrationWizard extends Wizard {

	public UVLbasedIntegrationWizard(OOSEMProject project, OOSEMBlock block) {
		setWindowTitle("UVL based Integration");
		this.project = project;
		this.block = block;
	}

	@Override
	public void addPages() {
		page = new UVLbasedIntegrationPage(project, block);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		page.refreshDataFromUI();
 
		String extension = page.getConfigFileExtension();
		Optional<ConfigurationReader> reader = ConfigurationReaderRegistry.getReader(extension);
		if (reader.isEmpty()) {
			MessageDialog.openError(getShell(), "Unsupported Configuration File",
					"Configuration files with extension \"" + extension + "\" are not supported yet.");
			return false;
		}
 
		ConfigurationSelection selection;
		try {
			selection = reader.get().readSelection(new File(page.getConfigFilePath()));
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "UVL based Integration Failed",
					"Could not read the configuration file: " + e.getMessage());
			return false;
		}
 
		var config = new BlockConfigurationData(block);
		var processor = new UVLconfigProcessor(project, config, selection);
		var blockGenData = processor.getBlockGenerationData();
		if(blockGenData != null) {
			blockGenData.setBlockName(page.getElementName());
			var content = OOSEMBlockGenerator.generate(blockGenData);
			SysMLFileWriter.writeFile(blockGenData, content);
		} else {
			System.err.println(processor.getErrors());
		}
 
		return true;
	}

	private UVLbasedIntegrationPage page;
	private final OOSEMProject project;
	private final OOSEMBlock block;
}