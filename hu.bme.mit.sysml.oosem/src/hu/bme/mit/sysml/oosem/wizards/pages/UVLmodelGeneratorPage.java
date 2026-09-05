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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import hu.bme.mit.sysml.oosem.model.elements.OOSEMBlock;
import hu.bme.mit.sysml.oosem.model.project.interfaces.OOSEMProject;
import hu.bme.mit.sysml.oosem.util.OpenInFileUtils;

public class UVLmodelGeneratorPage extends WizardPage {

	public UVLmodelGeneratorPage(OOSEMProject project, OOSEMBlock block) {
		super("UVL Model Generator Wizard");
		setTitle("UVL Model Generator Page");
		setDescription("Helps in generating a UVL (Universal Variability Language) model file.");
		this.fileName = block.getName();
		this.location = OpenInFileUtils.getFileForEObject(block.getObject()).getParent().getLocation().toString();
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		new Label(container, SWT.NONE).setText("File name:");
		fileNameText = new Text(container, SWT.BORDER);
		fileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fileNameText.setText(fileName);
		fileNameText.addListener(SWT.Modify, event -> {
			if (event.doit)
				validateLocation();
		});

		new Label(container, SWT.NONE).setText("Location:");
		Composite browseComp = new Composite(container, SWT.NONE);
		browseComp.setLayout(new GridLayout(2, false));
		browseComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		locationText = new Text(browseComp, SWT.BORDER);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		locationText.setText(location);
		locationText.addListener(SWT.Modify, event -> {
			if (event.doit)
				validateLocation();
		});
		Button browseBtn = new Button(browseComp, SWT.PUSH);
		browseBtn.setText("Browse...");
		browseBtn.addListener(SWT.Selection, e -> {
			DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
			dialog.setFilterPath(location);
			String path = dialog.open();
			if (path != null)
				locationText.setText(path);
		});

		refreshDataFromUI();
		validateLocation();
		setControl(container);
	}

	public void refreshDataFromUI() {
		fileName = fileNameText.getText();
		location = locationText.getText();
	}

	private void validateLocation() {
		var path = locationText.getText() + "/" + fileNameText.getText() + ".uvl";
		if (new File(path).exists()) {
			setMessage("File already exists at location", IMessageProvider.ERROR);
			setPageComplete(false);
		} else {
			setMessage(null);
			setPageComplete(true);
		}
	}

	public String getFileName() {
		return fileName;
	}

	public String getLocation() {
		return location;
	}

	private Text fileNameText;
	private Text locationText;
	private Composite container;

	private String fileName;
	private String location;
}