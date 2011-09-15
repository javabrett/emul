/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.ast.IAstStringLitExpr;
import org.ejs.eulang.types.LLType;

/**
 * @author ejs
 *
 */
public class AstStringLitExpr extends AstLitExpr implements IAstStringLitExpr {

	/**
	 * @param lit
	 * @param type
	 */
	public AstStringLitExpr(String lit, LLType type) {
		super(lit, type);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstTypedNode#toString()
	 */
	@Override
	public String toString() {
		return typedString('"' + getLiteral() + '"');
	}
	
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstStringLitExpr#copy()
	 */
	@Override
	public IAstStringLitExpr copy() {
		return new AstStringLitExpr(getValue(), type);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstStringLitExpr#getValue()
	 */
	@Override
	public String getValue() {
		return getLiteral();
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstLitExpr#getObject()
	 */
	@Override
	public Object getObject() {
		return getLiteral();
	}


	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstLitExpr#isZero()
	 */
	@Override
	public boolean isZero() {
		return getValue().length() == 0;
	}
}