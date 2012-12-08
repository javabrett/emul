/**
 * 
 */
package v9t9.gui.client.swt.shells;

import java.text.MessageFormat;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import v9t9.common.files.EmulatedFile;
import v9t9.gui.client.swt.shells.disk.ByteContentViewer;

/**
 * @author ejs
 *
 */
public class FileContentComposite extends Composite {

	private Label summaryLabel;
	private ByteContentViewer contentViewer;

	public FileContentComposite(Composite parent, int style) {
		super(parent, style);
		
		GridLayoutFactory.fillDefaults().applyTo(this);
		
		summaryLabel = new Label(this, SWT.WRAP);
		summaryLabel.setText("No file");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(summaryLabel);
		
		contentViewer = new ByteContentViewer(this, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(contentViewer);
	}
	
	public void setFile(EmulatedFile file) {
		summaryLabel.setText(MessageFormat.format("File: {0}; Size = {1} sectors ({2} bytes)",
				file.getFileName(), (file.getSectorsUsed() + 1), file.getFileSize()));
		
		contentViewer.setFile(file);
	}
}