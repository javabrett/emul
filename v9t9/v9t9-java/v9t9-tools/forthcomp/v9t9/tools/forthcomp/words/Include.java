/*
  Include.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.forthcomp.words;

import java.io.File;
import java.io.FileNotFoundException;

import v9t9.tools.forthcomp.AbortException;
import v9t9.tools.forthcomp.HostContext;
import v9t9.tools.forthcomp.ISemantics;
import v9t9.tools.forthcomp.TargetContext;

/**
 * @author ejs
 *
 */
public class Include extends BaseWord {
	public Include() {
		setExecutionSemantics(new ISemantics() {
			
			public void execute(HostContext hostContext, TargetContext targetContext)
					throws AbortException {
				String filename = hostContext.readToken();
				
				try {
					File dir = new File(hostContext.getStream().getFile()).getParentFile();
					File file = new File(dir, filename);
					if (file.exists())
						hostContext.getStream().push(file);
					else
						hostContext.getStream().push(new File(filename));
				} catch (FileNotFoundException e) {
					throw hostContext.abort(e.getMessage());
				}				
			}
		});
		setCompilationSemantics(getExecutionSemantics());
	}
}
