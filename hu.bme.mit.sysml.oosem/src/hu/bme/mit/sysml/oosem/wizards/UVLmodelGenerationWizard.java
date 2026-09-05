package hu.bme.mit.sysml.oosem.wizards;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;

import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData;
import hu.bme.mit.sysml.oosem.generators.UVLgenerator;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMBlock;
import hu.bme.mit.sysml.oosem.model.project.interfaces.OOSEMProject;
import hu.bme.mit.sysml.oosem.wizards.pages.UVLmodelGeneratorPage;

public class UVLmodelGenerationWizard extends Wizard {

	public UVLmodelGenerationWizard(OOSEMProject project, OOSEMBlock block) {
		setWindowTitle("UVL Model Generator");
		this.project = project;
		this.block = block;
	}

	@Override
	public void addPages() {
		page = new UVLmodelGeneratorPage(project, block);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		page.refreshDataFromUI();

		String fileName = page.getFileName();
		String location = page.getLocation();
		File outputFile = new File(location, fileName + ".uvl");

		try {
			generateUVLModel(outputFile);
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "UVL Model Generation Failed",
					"Could not write the UVL model file: " + e.getMessage());
			return false;
		}

		return true;
	}

	private void generateUVLModel(File outputFile) throws IOException {
		File parent = outputFile.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		try (FileWriter writer = new FileWriter(outputFile)) {
			var blockConfigData = new BlockConfigurationData(block);
			var content = new UVLgenerator().generate(blockConfigData);
			writer.write(content);
		}
	}

	private UVLmodelGeneratorPage page;
	private final OOSEMProject project;
	private final OOSEMBlock block;
}