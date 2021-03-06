/*
  HostPostponedWord.java

  (c) 2010-2014 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.forthcomp.words;

import java.util.Stack;

import v9t9.tools.forthcomp.AbortException;
import v9t9.tools.forthcomp.HostContext;
import v9t9.tools.forthcomp.ISemantics;
import v9t9.tools.forthcomp.ITargetWord;
import v9t9.tools.forthcomp.IWord;
import v9t9.tools.forthcomp.TargetContext;

/**
 * @author ejs
 *
 */
public class HostPostponedWord extends BaseWord {

	private final ITargetWord targetWord;
	private final IWord hostWord;

	/**
	 * @param i
	 */
	public HostPostponedWord(IWord hostWord_, ITargetWord targetWord_) {
		this.hostWord = hostWord_;
		this.targetWord = targetWord_;
		setName(targetWord.getEntry().getName());
		
		setExecutionSemantics(new ISemantics() {
			
			@Override
			public void execute(HostContext hostContext, TargetContext targetContext)
					throws AbortException {
				//hostContext.compileWord(targetContext, hostWord, targetWord);
				if (!targetWord.getEntry().isTargetOnly()) {
					IWord hostBehavior = targetWord.getEntry().getHostBehavior();
					if (hostBehavior != null) {
						int dp = 0;
						Stack<Integer> origDataStack = null;
						Stack<Integer> origReturnStack = null;
						origDataStack = new Stack<Integer>(); 
						origDataStack.addAll(hostContext.getDataStack());
						origReturnStack = new Stack<Integer>();
						origReturnStack.addAll(hostContext.getReturnStack());
						dp = targetContext.getDP();
						
						targetWord.getExecutionSemantics().execute(hostContext, targetContext);
						
						targetContext.setDP(dp);
						hostContext.getDataStack().clear();
						hostContext.getDataStack().addAll(origDataStack);
						hostContext.getReturnStack().clear();
						hostContext.getReturnStack().addAll(origReturnStack);
						
						if (hostContext.isCompiling())
							hostBehavior.getCompilationSemantics().execute(hostContext, targetContext);
						else
							hostBehavior.getExecutionSemantics().execute(hostContext, targetContext);
					} else {
//						if (hostContext.isCompiling() && targetWord.getExecutionSemantics() == null) {
//							targetWord.getCompilationSemantics().execute(hostContext, targetContext);
//							hostWord.getCompilationSemantics().execute(hostContext, targetContext);
//						} else {
//							targetWord.getExecutionSemantics().execute(hostContext, targetContext);
//						}
						hostContext.compileWord(targetContext, hostWord, targetWord);
					}
				} else if (hostWord != null) {
//					if (hostContext.isCompiling()) {
//						hostWord.getCompilationSemantics().execute(hostContext, targetContext);
//					} else {
//						hostWord.getExecutionSemantics().execute(hostContext, targetContext);
//					}
					hostContext.compileWord(targetContext, hostWord, targetWord);
				} else {
					throw hostContext.abort("cannot invoke POSTPONE'd word -- no host-only definition: " + targetWord.getName());
				}
			}
		});
	}
	

}
