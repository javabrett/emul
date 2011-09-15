/**
 * 
 */
package org.ejs.eulang.symbols;

import org.ejs.coffee.core.utils.Check;
import org.ejs.eulang.ITyped;
import org.ejs.eulang.ast.IAstDefineStmt;
import org.ejs.eulang.ast.IAstName;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.types.LLSymbolType;
import org.ejs.eulang.types.LLType;

/**
 * @author ejs
 *
 */
public class Symbol implements ISymbol {

	private final String name;
	private String llvmName;
	private IAstNode def;
	private LLType type;
	private IScope scope;
	private int number;
	private boolean temp;
	private boolean addressed;
	private Visibility vis;
	
	public Symbol(int number, String name,  Visibility vis, LLType type, boolean temporary, IScope scope, IAstNode def, boolean addressed) {
		this.number = number;
		this.name = name;
		this.type = type;
		this.vis = vis;
		this.scope = scope;
		this.temp = temporary;
		this.addressed = addressed;
		Check.checkArg(this.name);
		setDefinition(def);
	}
	public Symbol(int number, IAstName name, IAstNode def) {
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
		Symbol other = (Symbol) obj;
		if (number != other.number)
			return false;
		if (temp != other.temp)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		/*
		if (def == null) {
			if (other.def != null)
				return false;
		} else if (def.getId() != other.def.getId())
			return false;
		*/
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		//return "\"" + name + "\"" + ":" +(type != null ? type.toString() : "<unknown>");
		return getUniqueName() + " [" +(type != null ? type.getName() : "<unknown>") + "]";
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getUniqueName()
	 */
	@Override
	public String getUniqueName() {
		return (name  + (temp ? "." + number : ""));
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
	@Override
	 */
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
		if (type != this.type) {
			// the symbol itself should not have a type for defines
			assert !(def instanceof IAstDefineStmt);
			if (type instanceof LLSymbolType && ((LLSymbolType) type).getSymbol() == this)
				return;
			//assert !(scope.getOwner() instanceof IAstDefineStmt); 
			this.type = type;
			llvmName = null;
		}
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
		if (temp != this.temp) {
			getScope().remove(this);
			this.temp = temp;
			if (temp)
				this.number = scope.nextId();
			getScope().add(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#isAddressed()
	 */
	@Override
	public boolean isAddressed() {
		return addressed;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#setAddressed(boolean)
	 */
	@Override
	public void setAddressed(boolean addressed) {
		this.addressed = addressed;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getVisibility()
	 */
	@Override
	public Visibility getVisibility() {
		return vis;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#setVisibility(org.ejs.eulang.symbols.ISymbol.Visibility)
	 */
	@Override
	public void setVisibility(Visibility vis) {
		this.vis = vis;
		llvmName = null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.symbols.ISymbol#getLLVMName()
	 */
	@Override
	public String getLLVMName() {
		if (llvmName == null) {
			String prefix;
			switch (vis) { 
			case GLOBAL:
			case MODULE:
			case NAMESPACE:
				prefix = "@";
				break;
			case LOCAL:
				prefix = "%";
				break;
			default:
				throw new IllegalStateException();
			}
			String safeName = getUniqueName().replace(" => ","$");
			safeName = safeName.replaceAll("[^a-zA-Z0-9_$,]", ".");
			llvmName = prefix + (scope != null && vis != Visibility.LOCAL ? scope.getUniqueName() : "") + safeName;
		}
		return llvmName;
	}
}