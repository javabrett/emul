/**
 * 
 */
package org.ejs.eulang.ast;

/**
 * @author ejs
 *
 */
public interface IAstRefType extends IAstType {
	IAstRefType copy();
	
	IAstType getBaseType();
	void setBaseType(IAstType typeExpr);
}