/**
 * 
 */
package org.ejs.eulang.symbols;

import org.ejs.coffee.core.utils.Check;
import org.ejs.eulang.ITyped;
import org.ejs.eulang.ast.IAstName;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.types.LLType;

/**
 * @author ejs
 *
 */
public abstract class BaseSymbol implements ISymbol {

	private String name;
	private IAstNode def;
	private LLType type;
	private IScope scope;
	private int number;
	private boolean temp;
	
	public BaseSymbol(int number, String name, boolean temporary, IScope scope, IAstNode def) {
		this.number = number;
		this.name = name;
		this.scope = scope;
		this.temp = temporary;
		Check.checkArg(this.name);
		setDefinition(def);
	}
	public BaseSymbol(int number, IAstName name, IAstNode def) {
		this.number = number;
		this.name = name.getName();
		this.scope = name.getScope();
		setDefinition(def);
	}
	
	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + number;
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
		BaseSymbol other = (BaseSymbol) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number != other.number)
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		//return "\"" + name + "\"" + ":" +(type != null ? type.toString() : "<unknown>");
		return getUniqueName() + " [" +(type != null ? type.toString() : "<unknown>") + "]";
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getUniqueName()
	 */
	@Override
	public String getUniqueName() {
		return name  + (temp ? "." + number : "");
	}
	
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getScope()
	 */
	@Override
	public IScope getScope() {
		return scope;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#setScope(org.ejs.eulang.symbols.IScope)
	 */
	@Override
	public void setScope(IScope scope) {
		this.scope = scope;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getDefinition()
	 */
	@Override
	public IAstNode getDefinition() {
		return def;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#setDefinition(org.ejs.eulang.ast.IAstNode)
	 */
	@Override
	public void setDefinition(IAstNode def) {
		this.def = def;
		if (this.type == null)
			this.type = def instanceof ITyped ? ((ITyped) def).getType() : null;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getType()
	 */
	@Override
	public LLType getType() {
		return type;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#setType(org.ejs.eulang.types.LLType)
	 */
	@Override
	public void setType(LLType type) {
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#isTemporary()
	 */
	@Override
	public boolean isTemporary() {
		return temp;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#setTemporary(boolean)
	 */
	@Override
	public void setTemporary(boolean temp) {
		this.temp = temp;
		if (temp)
			this.number = scope.nextId();
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getLLVMName()
	 */
	@Override
	public String getLLVMName() {
		String prefix = "@";
		if (getScope() instanceof LocalScope)
			prefix = "%";
		return prefix + getName().replaceAll("@", "\\$");
	}
}
