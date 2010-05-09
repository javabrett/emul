/**
 * 
 */
package org.ejs.eulang.ast;

/**
 * A loop with a test expression and a body
 * @author ejs
 *
 */
public interface IAstBodyLoopExpr extends IAstLoopStmt {
	 
	IAstTypedExpr getExpr();
	void setExpr(IAstTypedExpr expr);
}
