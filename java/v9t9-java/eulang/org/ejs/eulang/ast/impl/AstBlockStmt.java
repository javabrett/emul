/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.ast.IAstBlockStmt;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstNodeList;
import org.ejs.eulang.ast.IAstStmt;
import org.ejs.eulang.ast.IAstType;
import org.ejs.eulang.symbols.IScope;

/**
 * @author ejs
 *
 */
public class AstBlockStmt extends AstScope implements IAstBlockStmt {

	private IAstNodeList<IAstStmt> stmtList;

	/**
	 * @param stmtList
	 * @param scope 
	 */
	public AstBlockStmt(IAstNodeList<IAstStmt> stmtList, IScope scope) {
		super(scope);
		this.stmtList = stmtList;
		stmtList.setParent(this);
	}

	public IAstBlockStmt copy(IAstNode copyParent) {
		IAstBlockStmt copied = new AstBlockStmt(doCopy(stmtList, copyParent), getScope().newInstance(getScope()));
		remapScope(getScope(), copied.getScope(), copied);
		return fixup(this, copied);
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#toString()
	 */
	@Override
	public String toString() {
		return "BLOCK";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 444;
		result = prime * result
				+ ((stmtList == null) ? 0 : stmtList.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		AstBlockStmt other = (AstBlockStmt) obj;
		if (stmtList == null) {
			if (other.stmtList != null)
				return false;
		} else if (!stmtList.equals(other.stmtList))
			return false;
		return true;
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
}
