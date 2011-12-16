/**
 * 
 */
package v9t9.gui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ejs.base.properties.IProperty;


/**
 * @author ejs
 *
 */
public class DisplayPropertyEditor implements IPropertyEditor {

	private final IProperty property;

	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.editor.IPropertyEditor#createEditor(org.eclipse.swt.widgets.Composite)
	 */
	/**
	 * @param readOnlyFieldProperty
	 */
	public DisplayPropertyEditor(IProperty property) {
		this.property = property;
	}

	public IPropertyEditorControl createEditor(Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(property.getValue().toString());
		return new IPropertyEditorControl() {

			@Override
			public Control getControl() {
				return label;
			}

			@Override
			public void reset() {
			}
			
		};
	}

}
