/**
 * 
 */
package v9t9.forthcomp.words;

import v9t9.forthcomp.AbortException;
import v9t9.forthcomp.DictEntry;
import v9t9.forthcomp.HostContext;
import v9t9.forthcomp.ISemantics;

/**
 * @author ejs
 *
 */
public class TargetVariable extends TargetWord {

	/**
	 * @param addr 
	 * 
	 */
	public TargetVariable(DictEntry entry) {
		super(entry);
		setCompilationSemantics(new ISemantics() {
			
			public void execute(HostContext hostContext, TargetContext targetContext)
					throws AbortException {
				if (getEntry().canInline())
					targetContext.compileLiteral(getEntry().getParamAddr(), false, true);
				else
					targetContext.compile(TargetVariable.this);
			}
		});
		setExecutionSemantics(new ISemantics() {
			
			public void execute(HostContext hostContext, TargetContext targetContext)
			throws AbortException {
				hostContext.pushData(getEntry().getParamAddr());
			}
		});
	}
	
}