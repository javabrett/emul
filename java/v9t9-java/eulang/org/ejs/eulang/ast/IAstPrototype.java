/**
 * 
 */
package org.ejs.eulang.ast;

import org.ejs.eulang.types.LLCodeType;


/**
 * @author ejs
 *
 */
public interface IAstPrototype extends IAstTypedNode {
	IAstPrototype copy(IAstNode copyParent);
	
	IAstType returnType();
	
	IAstArgDef[] argumentTypes();

	int getArgCount();
	boolean adaptToType(LLCodeType newType);

}
