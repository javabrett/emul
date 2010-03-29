/**
 * 
 */
package org.ejs.eulang.ast.impl;

import java.util.ArrayList;
import java.util.List;

import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstScope;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;


/**
 * @author ejs
 *
 */
public abstract class AstScope extends AstNode implements IAstScope {
	private IScope scope;

	/**
	 * 
	 */
	public AstScope(IScope scope) {
		this.scope = scope;
		scope.setOwner(this);
	}
   
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 22;
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		AstScope other = (AstScope) obj;
		if (!scope.equals(other.scope))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.IAstNode#getChildren()
	 */
	@Override
	public IAstNode[] getChildren() {
		ISymbol[] symbols = scope.getSymbols();
		List<IAstNode> kids = new ArrayList<IAstNode>(symbols.length);
		for (int i = 0; i < symbols.length; i++) {
			IAstNode kid = symbols[i].getDefinition();
			if (kid != null)
				kids.add(kid);
		}
		return (IAstNode[]) kids.toArray(new IAstNode[kids.size()]);
		
	}
	
	@Override
	public void replaceChildren(IAstNode[] children) {
		throw new UnsupportedOperationException();
	}
	
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstScope#getScope()
	 */
	@Override
	public IScope getScope() {
		return scope;
	}
	
	@Override
	public void setParent(IAstNode node) {
		super.setParent(node);

		if (node != null) {
			while (node != null) {
				if (node instanceof IAstScope) {
					((IAstScope) node).getScope().setParent(scope);
					break;
				}
				node = node.getParent();
			}
		} else {
			scope.setParent(null);
		}
		
	}
	
}
