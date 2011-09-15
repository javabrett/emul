/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.IAstAddrOfExpr;
import org.ejs.eulang.ast.IAstDerefExpr;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstTypedExpr;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeException;


/**
 * @author ejs
 *
 */
public class AstAddrOfExpr extends AstTypedExpr implements IAstAddrOfExpr {

	private IAstTypedExpr expr;

	/**
	 * @param type
	 */
	public AstAddrOfExpr(IAstTypedExpr expr) {
		setExpr(expr);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#copy()
	 */
	@Override
	public IAstAddrOfExpr copy() {
		return fixup(this, new AstAddrOfExpr(doCopy(expr)));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expr == null) ? 0 : expr.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstAddrOfExpr other = (AstAddrOfExpr) obj;
		if (expr == null) {
			if (other.expr != null)
				return false;
		} else if (!expr.equals(other.expr))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.impl.AstNode#toString()
	 */
	@Override
	public String toString() {
		return typedString("ADDROF");
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstType#getChildren()
	 */
	@Override
	public IAstNode[] getChildren() {
		return new IAstNode[] { expr };
	}
	
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#replaceChildren(org.ejs.eulang.ast.IAstNode[])
	 */
	@Override
	public void replaceChild(IAstNode existing, IAstNode another) {
		if (existing == expr)
			setExpr((IAstTypedExpr) another);
		else
			throw new IllegalArgumentException();
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstType#setSymbol(org.ejs.eulang.ast.IAstSymbolExpr)
	 */
	@Override
	public void setExpr(IAstTypedExpr expr) {
		this.expr = reparent(this.expr, expr);
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstType#getSymbol()
	 */
	@Override
	public IAstTypedExpr getExpr() {
		return expr;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstTypedNode#inferTypeFromChildren(org.ejs.eulang.ast.TypeEngine)
	 */
	@Override
	public boolean inferTypeFromChildren(TypeEngine typeEngine)
			throws TypeException {
		boolean changed = false;
		
		// the type is fixed to be the pointer-to the dereferenced child.
		if (canInferTypeFrom(expr)) {
			LLType child = typeEngine.getBaseType(expr.getType());
			child = typeEngine.getPointerType(child);
			changed |= updateType(this, child);
			
		}
		
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#simplify(org.ejs.eulang.TypeEngine)
	 */
	@Override
	public boolean simplify(TypeEngine engine) {
		boolean changed = super.simplify(engine); 
		if (expr instanceof IAstAddrOfExpr) {
			getParent().replaceChild(this, expr);
			return true;
		}
		if (expr instanceof IAstDerefExpr) {
			((IAstDerefExpr) expr).getExpr().setParent(null);
			replaceChild(expr, ((IAstDerefExpr) expr).getExpr());
			return true;
		}
		return changed; 
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#validateChildTypes(org.ejs.eulang.TypeEngine)
	 */
	@Override
	public void validateChildTypes(TypeEngine typeEngine) throws TypeException {
		if (getType() != null && getType().isComplete() && expr.getType() != null) {
			if (!expr.getType().equals(getType().getSubType()))
				throw new TypeException(expr, "pointer base type does not match in parent");
		}
	}

}