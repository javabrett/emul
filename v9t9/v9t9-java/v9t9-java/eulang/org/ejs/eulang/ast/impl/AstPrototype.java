/**
 * 
 */
package org.ejs.eulang.ast.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.IAstArgDef;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstPrototype;
import org.ejs.eulang.ast.IAstType;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;
import org.ejs.eulang.types.LLCodeType;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeException;


/**
 * @author ejs
 *
 */
public class AstPrototype extends AstTypedExpr implements IAstPrototype {
	private IAstType retType;
	private IAstArgDef[] argumentTypes;
	private Set<String> attrs;

	/** Create with the types; may be null 
	 * @param attrs */
	public AstPrototype(TypeEngine typeEngine, IAstType retType, IAstArgDef[] argumentTypes, Set<String> attrs) {
		this.retType = retType;
		retType.setParent(this);
		this.argumentTypes = argumentTypes;
		for (IAstArgDef arg : argumentTypes)
			arg.setParent(this);
		
		setType(typeEngine.getCodeType(retType, argumentTypes));
		this.attrs = attrs;
	}
	protected AstPrototype(LLType type, IAstType retType, IAstArgDef[] argumentTypes, Set<String> attrs) {
		this.retType = retType;
		retType.setParent(this);
		this.argumentTypes = argumentTypes;
		for (IAstArgDef arg : argumentTypes)
			arg.setParent(this);
		setType(type);
		this.attrs = attrs;
	}
	
	/**
	 * @param argCode
	 */
	public AstPrototype(LLCodeType codeType, IScope argNameScope, String[] argNames) {
		this.retType = new AstType(codeType.getRetType());
		this.retType.setParent(this);
		
		this.argumentTypes = new IAstArgDef[codeType.getArgTypes().length];
		for (int i = 0; i < codeType.getArgTypes().length; i++) {
			ISymbol argName = argNameScope.add(argNames[i], false);
			AstType argType = new AstType(codeType.getArgTypes()[i]);
			this.argumentTypes[i] = new AstArgDef(new AstSymbolExpr(true, argName), 
					argType,
					null /*Default*/,
					Collections.<String>emptySet());
			argName.setDefinition(this.argumentTypes[i]);
			this.argumentTypes[i].setParent(this);
		}
		this.attrs = Collections.<String>emptySet();
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#copy()
	 */
	@Override
	public IAstPrototype copy() {
		IAstArgDef[] argTypesCopy = new IAstArgDef[argumentTypes.length];
		for (int i = 0; i < argTypesCopy.length; i++) {
			argTypesCopy[i] = argumentTypes[i].copy();
			argTypesCopy[i] = fixup(argumentTypes[i], argTypesCopy[i]);
		}
		return fixup(this, new AstPrototype(getType(), doCopy(returnType()), argTypesCopy, new HashSet<String>(attrs)));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 55;
		result = prime * result + Arrays.hashCode(argumentTypes);
		result = prime * result + ((retType == null) ? 0 : retType.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		AstPrototype other = (AstPrototype) obj;
		if (!Arrays.equals(argumentTypes, other.argumentTypes))
			return false;
		if (retType == null) {
			if (other.retType != null)
				return false;
		} else if (!retType.equals(other.retType))
			return false;
		return true;
	}



	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.impl.AstNode#toString()
	 */
	@Override
	public String toString() {
		return "(" + catenate(argumentTypes) + " => " + (retType != null ? retType.toString() : "<Object>") + ")"; 
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstCodeExpression#argumentTypes()
	 */
	@Override
	public IAstArgDef[] argumentTypes() {
		return argumentTypes;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstCodeExpression#getReturnType()
	 */
	@Override
	public IAstType returnType() {
		return retType;
	}
	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.IAstNode#getChildren()
	 */
	@Override
	public IAstNode[] getChildren() {
		IAstNode[] children = new IAstNode[argumentTypes.length + 1];
		children[0] = retType;
		System.arraycopy(argumentTypes, 0, children, 1, argumentTypes.length);
		return children;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#replaceChildren(org.ejs.eulang.ast.IAstNode[])
	 */
	@Override
	public void replaceChild(IAstNode existing, IAstNode another) {
		if (returnType() == existing) {
			retType = (IAstType) another;
			if (retType != null)
				retType.setParent(this);
			return;
		}
		for (int i = 0; i < argumentTypes.length; i++) {
			IAstArgDef argDef = argumentTypes[i];
			if (argDef == existing) {
				argumentTypes[i] = (IAstArgDef) another;
				if (argumentTypes[i] != null)
					argumentTypes[i].setParent(this);
				return;
			}
		}
		throw new IllegalArgumentException();
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstTypedNode#inferTypeFromChildren(org.ejs.eulang.ast.TypeEngine)
	 */
	@Override
	public boolean inferTypeFromChildren(TypeEngine typeEngine)
			throws TypeException {
		LLCodeType newType = null;
		
		LLType[] argTypes = new LLType[argumentTypes.length];
		LLType tretType = retType.getType();
		for (int i = 0; i < argumentTypes.length; i++) {
			IAstArgDef argDef = argumentTypes[i];
			argTypes[i] = argDef.getType();
		}
		
		newType = typeEngine.getCodeType(tretType, argTypes);
		
		return updateType(this, newType);
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstPrototype#adaptToType(org.ejs.eulang.types.LLType)
	 */
	@Override
	public boolean adaptToType(LLCodeType codeType) {
		boolean changed = false;
		changed = AstTypedNode.updateType(retType, codeType.getRetType());
		LLType[] argTypes = codeType.getArgTypes();
		for (int  i = 0; i < argumentTypes.length; i++) {
			if (i >= argTypes.length)
				break;
			changed |= AstTypedNode.updateType(argumentTypes[i], argTypes[i]);
		}
		return changed;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstPrototype#getArgCount()
	 */
	@Override
	public int getArgCount() {
		return argumentTypes.length;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstPrototype#hasDefaultArguments()
	 */
	@Override
	public boolean hasDefaultArguments() {
		for (int  i = 0; i < argumentTypes.length; i++) {
			if (argumentTypes[i].getDefaultValue() != null)
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#validateChildTypes()
	 */
	@Override
	public void validateChildTypes(TypeEngine typeEngine) throws TypeException {
		// no restrictions
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstPrototype#addArgument(int, org.ejs.eulang.ast.IAstArgDef)
	 */
	@Override
	public void addArgument(int position, IAstArgDef newArgDef) {
		IAstArgDef[] newArgTypes = new IAstArgDef[argumentTypes.length + 1];
		System.arraycopy(argumentTypes, 0, newArgTypes, 0, position);
		System.arraycopy(argumentTypes, position, newArgTypes, position + 1, argumentTypes.length - position);
		newArgTypes[position] = newArgDef;
		newArgDef.setParent(this);
		this.argumentTypes = newArgTypes;
		setType(null);
	}
	

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAttributes#getAttrs()
	 */
	@Override
	public Set<String> getAttrs() {
		return Collections.unmodifiableSet(attrs);
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAttributes#attrs()
	 */
	@Override
	public Set<String> attrs() {
		if (attrs == Collections.<String>emptySet())
			attrs = new HashSet<String>();
		return attrs;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAttributes#hasAttr(java.lang.String)
	 */
	@Override
	public boolean hasAttr(String attr) {
		return attrs.contains(attr);
	}
}