/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.ASTException;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstNodeList;
import org.ejs.eulang.ast.IAstScope;
import org.ejs.eulang.ast.IAstStmt;
import org.ejs.eulang.ast.IAstStmtScope;
import org.ejs.eulang.ast.IAstType;
import org.ejs.eulang.ast.IAstTypedNode;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.types.TypeException;

/**
 * @author ejs
 *
 */
public class AstStmtScope extends AstTypedExpr implements IAstStmtScope, IAstTypedNode {

	protected IAstNodeList<IAstStmt> stmtList;

	protected IScope scope;
	/**
	 * @param stmtList
	 * @param scope 
	 */
	public AstStmtScope(IAstNodeList<IAstStmt> stmtList, IScope scope) {
		this.scope = scope;
		scope.setOwner(this);
		this.stmtList = reparent(this.stmtList, stmtList);
		
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#copy(org.ejs.eulang.ast.IAstNode)
	 */
	@Override
	public IAstNode copy(IAstNode copyParent) {
		return fixupStmtScope(new AstStmtScope(doCopy(stmtList, copyParent), scope.newInstance(getCopyScope(copyParent))));
	}
	protected IAstStmtScope fixupStmtScope(IAstStmtScope copied) {
		remapScope(getScope(), copied.getScope(), copied);
		return fixup(this, copied);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 444;
		result = prime * result
				+ ((stmtList == null) ? 0 : stmtList.hashCode());
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstStmtScope other = (AstStmtScope) obj;
		if (stmtList == null) {
			if (other.stmtList != null)
				return false;
		} else if (!stmtList.equals(other.stmtList))
			return false;
		if (scope == null) {
			if (other.scope != null)
				return false;
		} else if (!scope.equals(other.scope))
			return false;
		return true;
	}

	@Override
	public void setParent(IAstNode node) {
		super.setParent(node);

		if (node != null) {
			while (node != null) {
				if (node instanceof IAstScope) {
					scope.setParent(((IAstScope) node).getScope());
					break;
				}
				node = node.getParent();
			}
		} else {
			scope.setParent(null);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstScope#getScope()
	 */
	@Override
	public IScope getScope() {
		return scope;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstBlockStmt#stmtList()
	 */
	@Override
	public IAstNodeList<IAstStmt> stmts() {
		return stmtList;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#getChildren()
	 */
	@Override
	public IAstNode[] getChildren() {
		return new IAstNode[] { stmtList };
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#replaceChildren(org.ejs.eulang.ast.IAstNode[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void replaceChild(IAstNode existing, IAstNode another) {
		if (stmtList == existing) {
			stmtList = (IAstNodeList<IAstStmt>) ((IAstType) another);
		} else {
			throw new IllegalArgumentException();
		}
	}

	/** Override in subclasses.  This sets the scope to VOID. */
	@Override
	public boolean inferTypeFromChildren(TypeEngine typeEngine)
			throws TypeException {
		boolean changed = false;
		if (canReplaceType(this)) {
			setType(typeEngine.VOID);
			changed = true;
		}
			
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstStmtScope#merge(org.ejs.eulang.ast.IAstStmtScope)
	 */
	@Override
	public void merge(IAstStmtScope added) throws ASTException {
		doMerge(stmtList, added.stmts());
	}

	protected <T extends IAstNode> void doMerge(IAstNodeList<T> nodes, IAstNodeList<T> added) throws ASTException {
		for (T node : added.list()) {
			node.reparent(nodes);
			nodes.add(node);
		}
		added.list().clear();
	}
	
}
