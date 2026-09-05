package hu.bme.mit.sysml.oosem.wizards.pages;

import java.io.File;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import hu.bme.mit.sysml.oosem.model.elements.OOSEMBlock;
import hu.bme.mit.sysml.oosem.model.project.interfaces.OOSEMProject;
import hu.bme.mit.sysml.oosem.util.OpenInFileUtils;

public class UVLbasedIntegrationPage extends WizardPage {

	public UVLbasedIntegrationPage(OOSEMProject project, OOSEMBlock block) {
		super("UVL based Integration Wizard");
		setTitle("UVL based Integration Page");
		setDescription("Helps in generating a design block/model from a UVL-derived configuration file.");
		this.elementName = "I_" + block.getName();
		this.defaultLocation = OpenInFileUtils.getFileForEObject(block.getObject()).getParent().getLocation().toString();
		this.outputDirectory = this.defaultLocation;
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		new Label(container, SWT.NONE).setText("Configuration file:");
		Composite configBrowseComp = new Composite(container, SWT.NONE);
		configBrowseComp.setLayout(new GridLayout(2, false));
		configBrowseComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		configFileText = new Text(configBrowseComp, SWT.BORDER);
		configFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		configFileText.addListener(SWT.Modify, event -> {
			if (event.doit)
				validate();
		});
		Button configBrowseBtn = new Button(configBrowseComp, SWT.PUSH);
		configBrowseBtn.setText("Browse...");
		configBrowseBtn.addListener(SWT.Selection, e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			dialog.setFilterExtensions(SUPPORTED_CONFIG_FILTERS);
			dialog.setFilterPath(defaultLocation);
			String path = dialog.open();
			if (path != null)
				configFileText.setText(path);
		});

		new Label(container, SWT.NONE).setText("New element name:");
		nameText = new Text(container, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		nameText.setText(elementName);
		nameText.addListener(SWT.Modify, event -> {
			if (event.doit)
				validate();
		});

		new Label(container, SWT.NONE).setText("Output directory:");
		Composite outputBrowseComp = new Composite(container, SWT.NONE);
		outputBrowseComp.setLayout(new GridLayout(2, false));
		outputBrowseComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		outputDirectoryText = new Text(outputBrowseComp, SWT.BORDER);
		outputDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		outputDirectoryText.setText(outputDirectory);
		outputDirectoryText.addListener(SWT.Modify, event -> {
			if (event.doit)
				validate();
		});
		Button outputBrowseBtn = new Button(outputBrowseComp, SWT.PUSH);
		outputBrowseBtn.setText("Browse...");
		outputBrowseBtn.addListener(SWT.Selection, e -> {
			DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
			dialog.setFilterPath(outputDirectoryText.getText());
			String path = dialog.open();
			if (path != null)
				outputDirectoryText.setText(path);
		});

		validate();
		setControl(container);
	}

	public void refreshDataFromUI() {
		configFilePath = configFileText.getText();
		elementName = nameText.getText();
		outputDirectory = outputDirectoryText.getText();
	}

	public String getConfigFileExtension() {
		String path = configFileText.getText();
		int dotIndex = path.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == path.length() - 1)
			return "";
		return path.substring(dotIndex + 1).toLowerCase();
	}

	public String getConfigFilePath() {
		return configFilePath;
	}

	public String getElementName() {
		return elementName;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public String getOutputModelPath() {
		return outputDirectory + File.separator + elementName + MODEL_FILE_EXTENSION;
	}

	private void validate() {
		String configPath = configFileText.getText();
		String name = nameText.getText();
		String directory = outputDirectoryText.getText();

		if (configPath.isBlank()) {
			setPageInvalid("Select a configuration file");
			return;
		}
		File configFile = new File(configPath);
		if (!configFile.exists() || !configFile.isFile()) {
			setPageInvalid("Configuration file does not exist");
			return;
		}

		if (name.isBlank()) {
			setPageInvalid("Specify a name for the new element");
			return;
		}

		if (directory.isBlank()) {
			setPageInvalid("Specify an output directory");
			return;
		}
		if (!new File(directory).isDirectory()) {
			setPageInvalid("Output directory does not exist");
			return;
		}

		File outputFile = new File(directory, name + MODEL_FILE_EXTENSION);
		if (outputFile.exists()) {
			setPageInvalid("File already exists at location");
			return;
		}

		setMessage(null);
		setPageComplete(true);
	}

	private void setPageInvalid(String message) {
		setMessage(message, IMessageProvider.ERROR);
		setPageComplete(false);
	}

	private static final String[] SUPPORTED_CONFIG_FILTERS = new String[] { "*.json", "*.*" };
	private static final String MODEL_FILE_EXTENSION = ".sysml";

	private Text configFileText;
	private Text nameText;
	private Text outputDirectoryText;
	private Composite container;

	private String defaultLocation;
	private String configFilePath;
	private String elementName;
	private String outputDirectory;

}