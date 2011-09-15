/**
 * 
 */
package org.ejs.eulang.ast;


/**
 * @author ejs
 *
 */
public interface IAstCodeExpr extends IAstTypedExpr, IAstStmtScope, IAstAttributes {
	IAstCodeExpr copy();
	
	IAstPrototype getPrototype();

	/**
	 * @param copy
	 */
	void setPrototype(IAstPrototype copy);
}