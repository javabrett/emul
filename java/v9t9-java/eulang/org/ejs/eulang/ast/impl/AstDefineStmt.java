/**
 * 
 */
package org.ejs.eulang.ast.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ejs.coffee.core.utils.Check;
import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.ASTException;
import org.ejs.eulang.ast.DumpAST;
import org.ejs.eulang.ast.ExpandAST;
import org.ejs.eulang.ast.IAstDataType;
import org.ejs.eulang.ast.IAstDefineStmt;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstNodeList;
import org.ejs.eulang.ast.IAstSymbolExpr;
import org.ejs.eulang.ast.IAstTypedExpr;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeException;


/**
 * @author ejs
 *
 */
public class AstDefineStmt extends AstScope implements IAstDefineStmt {

	private IAstSymbolExpr id;
	//private IAstTypedExpr expr;
	private IAstNodeList<IAstTypedExpr> bodyList;
	//private Map<LLType, IAstTypedExpr> typedBodyMap = new HashMap<LLType, IAstTypedExpr>();
	
	private Map<Integer, List<IAstTypedExpr>> instanceIdMap = new HashMap<Integer, List<IAstTypedExpr>>();
	private Map<LLType, List<ISymbol>> instanceTypeMap = new HashMap<LLType, List<ISymbol>>();
	private boolean generic;
	private Map<LLType, Map<List<IAstTypedExpr>, ISymbol>> instanceMap = 
		new HashMap<LLType, Map<List<IAstTypedExpr>, ISymbol>>();

	public AstDefineStmt(IAstSymbolExpr name, boolean generic, IScope scope, IAstNodeList<IAstTypedExpr> bodyList) {
		super(scope);
		this.id = name;
		id.setParent(this);
		setSymbolExpr(name);
		setGeneric(generic);
		this.bodyList = reparent(this.bodyList, bodyList);
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#copy()
	 */
	@Override
	public IAstDefineStmt copy(IAstNode copyParent) {
		// TODO: copy expansions
		return (IAstDefineStmt) fixupScope(new AstDefineStmt(doCopy(id, copyParent), isGeneric(), 
				scope.newInstance(getCopyScope(copyParent)),
				doCopy(bodyList, copyParent)));
	}

	


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((bodyList == null) ? 0 : bodyList.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (generic ? 19283 : 0);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null) return false;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstDefineStmt other = (AstDefineStmt) obj;
		if (generic != other.generic)
			return false;
		if (bodyList == null) {
			if (other.bodyList != null)
				return false;
		} else if (!bodyList.equals(other.bodyList))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.impl.AstNode#toString()
	 */
	@Override
	public String toString() {
		return "DEFINE " + getSymbol();
	}
	

	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.IAstNode#getChildren()
	 */
	@Override
	public IAstNode[] getChildren() {
		IAstNode[] kids = new IAstNode[2];
		kids[0] = id;
		kids[1] = bodyList;
		return kids;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#getDumpChildren()
	 */
	@Override
	public IAstNode[] getDumpChildren() {
		Collection<IAstTypedExpr> exprs = new ArrayList<IAstTypedExpr>(bodyList.list()); 
		exprs.addAll(getConcreteInstances());
		return (IAstTypedExpr[]) exprs.toArray(new IAstTypedExpr[exprs.size()]);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#replaceChild(org.ejs.eulang.ast.IAstNode, org.ejs.eulang.ast.IAstNode)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void replaceChild(IAstNode existing, IAstNode another) {
		if (getSymbolExpr() == existing) {
			setSymbolExpr((IAstSymbolExpr) another);
		}  else if (bodyList == existing) {
			this.bodyList =(IAstNodeList<IAstTypedExpr>) reparent(this.bodyList, another);
		}
		throw new IllegalArgumentException();
		
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#isGeneric()
	 */
	@Override
	public boolean isGeneric() {
		return generic;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#setGeneric(boolean)
	 */
	@Override
	public void setGeneric(boolean generic) {
		this.generic = generic;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAssignStmt#getExpr()
	 */
	/*
	@Override
	public IAstTypedExpr getExpr() {
		return bodyList.isEmpty() ? null : bodyList.get(0);
	}*/

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAssignStmt#getId()
	 */
	@Override
	public IAstSymbolExpr getSymbolExpr() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#getSymbol()
	 */
	@Override
	public ISymbol getSymbol() {
		return id.getSymbol();
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAssignStmt#setExpr(v9t9.tools.ast.expr.IAstExpression)
	 */
	/*
	@Override
	public void setExpr(IAstTypedExpr expr) {
		//this.expr = reparent(this.expr, expr);
		if (bodyList.isEmpty()) 
			bodyList.add(expr);
		else
			bodyList.set(0, expr);
		expr.setParent(this);
	}*/

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstAssignStmt#setId(v9t9.tools.ast.expr.IAstIdExpression)
	 */
	@Override
	public void setSymbolExpr(IAstSymbolExpr id) {
		Check.checkArg(id);
		this.id = reparent(this.id, id);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstTypedNode#inferTypeFromChildren()
	 */
	/*
	@Override
	public boolean inferTypeFromChildren(TypeEngine typeEngine) throws TypeException {
		return inferTypesFromChildren(new ITyped[] { expr, id });
	}
	*/

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#expansions()
	 */
	/*
	@Override
	public Map<ISymbol, IAstTypedExpr> instances() {
		return expansions;
	}
	*/
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#validateChildTypes()
	 */
	@Override
	public void validateChildTypes(TypeEngine typeEngine) throws TypeException {
		// don't worry about symbol type
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#bodyList()
	 */
	@Override
	public List<IAstTypedExpr> bodyList() {
		return bodyList.list();
	}
	
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#typedBodyMap()
	 */
	//@Override
	//public Map<LLType, IAstTypedExpr> typedBodyMap() {
	//	return typedBodyMap;
	//}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#instanceMap()
	 */
	@Override
	public Map<LLType, List<ISymbol>> bodyToInstanceMap() {
		Map<LLType, List<ISymbol>> map = new HashMap<LLType, List<ISymbol>>();
		for (Map.Entry<LLType, Map<List<IAstTypedExpr>, ISymbol>> entry : instanceMap.entrySet()) {
			List<ISymbol> list = new ArrayList<ISymbol>(entry.getValue().values());
			map.put(entry.getKey(), list);
		}
		for (Map.Entry<LLType, List<ISymbol>> entry : instanceTypeMap.entrySet()) {
			List<ISymbol> list = map.get(entry.getKey());
			if (list == null) {
				list = new ArrayList<ISymbol>();
				map.put(entry.getKey(), list);
			}
			list.addAll(entry.getValue());
		}
		return Collections.unmodifiableMap(instanceTypeMap);
	}
	
	protected boolean typeMatchesExactly(LLType orig, LLType target) {
		if (orig == null)
			return false;
		if (orig.matchesExactly(target))
			return true;
		return false;
	}
	protected boolean typeMatchesCompatible(LLType orig, LLType target) {
		if (orig == null || target == null)
			return false;
		return orig.isMoreComplete(target);
	}
	protected boolean typeMatchesGeneric(LLType orig, LLType target) {
		if (orig == null || target == null)
			return true;
		return target.isMoreComplete(orig);
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#getMatchingExpr(org.ejs.eulang.types.LLType)
	 */
	@Override
	public IAstTypedExpr getMatchingBodyExpr(LLType type) {
		if (bodyList.nodeCount() == 0)
			return null;
		
		if (type == null) {
			// then the first
			if (bodyList.nodeCount() == 1)
				return bodyList.getFirst();
			else
				return null;
		}
		
		// look for exact matches
		for (IAstTypedExpr expr : bodyList.list()) {
			if (typeMatchesExactly(expr.getType(), type))
				return expr;
		}	
		
		// then compatible ones
		for (IAstTypedExpr expr : bodyList.list()) {
			if (typeMatchesCompatible(expr.getType(), type))
				return expr;
		}
		
		// then generic matches
		for (IAstTypedExpr expr : bodyList.list()) {
			if (typeMatchesGeneric(expr.getType(), type))
				return expr;
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#getMatchingInstance(org.ejs.eulang.types.LLType)
	 */
	@Override
	public ISymbol getMatchingInstance(LLType bodyType, LLType instanceType) {
        if (bodyType == null || (bodyType.isComplete() && !bodyType.isGeneric())) {
        	throw new IllegalArgumentException();
        }
        
		List<ISymbol> list = instanceTypeMap.get(bodyType);
		if (list == null)
			return null;
		
		for (ISymbol instanceSym : list) {
			if (typeMatchesExactly(instanceSym.getType(), instanceType))
				return instanceSym;
		}
		/*
		for (IAstTypedExpr expr : list) {
			if (typeMatchesCompatible(expr.getType(), type))
				return expr;
		}
		for (IAstTypedExpr expr : list) {
			if (typeMatchesGeneric(expr.getType(), type))
				return expr;
		}
		*/
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#registerInstance(org.ejs.eulang.types.LLType, org.ejs.eulang.ast.IAstTypedExpr)
	 */
	@Override
	public ISymbol registerInstance(IAstTypedExpr body, IAstTypedExpr expansion) {
		//if (type == null || !type.isGeneric()) {
		//	throw new IllegalArgumentException();
		//}
		
		List<ISymbol> list = instanceTypeMap.get(body.getType());
		if (list == null) {
			list = new ArrayList<ISymbol>();
			//instanceIdMap.put(xbody.getId(), list);
			instanceTypeMap.put(body.getType(), list);
			//if (list == null)
			//	throw new IllegalArgumentException("type should match one inferred previously");
		}
		
		/*
		if (list == null) {
			list = instanceIdMap.get(body.getId());
			if (list == null) {
				
				//for (IAstTypedExpr body : bodyList.list()) {
				//	if (typeMatchesExactly(body.getType(), type)) {
						list = new ArrayList<IAstTypedExpr>();
						instanceIdMap.put(body.getId(), list);
						//instanceTypeMap.put(body.getType(), list);
				//		break;
				//	}
				//}
				if (list == null)
					throw new IllegalArgumentException("type should match one inferred previously");
			}
		}
		if (!list.contains(expansion)) {
			list.add(expansion);
			//expansion.setParent(this);
		}
		*/
		
		ISymbol expansionSym = getSymbol().getScope().addTemporary(getSymbol().getName());
		expansionSym.setDefinition(expansion);
		list.add(expansionSym);
		
		return expansionSym;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#getConcreteInstances()
	 */
	@Override
	public Collection<IAstTypedExpr> getConcreteInstances() {
		Set<IAstTypedExpr> list = new HashSet<IAstTypedExpr>();
		for (IAstTypedExpr expr : bodyList.list()) {
			if (expr.getType() != null && expr.getType().isComplete() && !expr.getType().isGeneric())
				list.add(expr);
		}
		for (List<IAstTypedExpr> alist : instanceIdMap.values()) {
			list.addAll(alist);
		}
		for (List<ISymbol> alist : instanceTypeMap.values()) {
			for (ISymbol sym : alist)
				list.add((IAstTypedExpr) sym.getDefinition());
		}
		return list;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#getGenericVariables()
	 */
	@Override
	public ISymbol[] getGenericVariables() {
		return scope.getSymbols();
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstDefineStmt#getInstanceForParameters(org.ejs.eulang.types.LLType, java.util.List)
	 */
	@Override
	public ISymbol getInstanceForParameters(TypeEngine typeEngine,
			LLType bodyType, List<IAstTypedExpr> instanceParams) throws ASTException {
		if (!isGeneric())
			throw new IllegalArgumentException();
		IAstTypedExpr body = getMatchingBodyExpr(bodyType);
		if (body == null)
			throw new IllegalArgumentException();
		
		if (instanceParams == null)
			throw new IllegalArgumentException();
		
		ISymbol[] varSymbols = getGenericVariables();
		if (instanceParams.size() != getScope().getSymbols().length) 
			throw new ASTException(instanceParams.isEmpty() ? this : instanceParams.get(0), 
					"must specify the same number of replacements as variables: " + instanceParams.size() 
					+ " != " + varSymbols.length);
		
		Map<List<IAstTypedExpr>, ISymbol> typeMap = instanceMap.get(bodyType);
		if (typeMap == null) {
			typeMap = new HashMap<List<IAstTypedExpr>, ISymbol>();
			instanceMap.put(bodyType, typeMap);
		}
		
		ISymbol symbol = getSymbol();
		
		ISymbol instanceSymbol = typeMap.get(instanceParams);
		if (instanceSymbol == null) {
			ExpandAST expand = new ExpandAST(typeEngine, true);
			IAstTypedExpr instance = expand.expandInstance(body, varSymbols, instanceParams);
			//typeMap.put(instanceParams, instance);
			
			instanceSymbol = updateInstanceBody(typeEngine, body, instance);
			typeMap.put(instanceParams, instanceSymbol);
			registerInstance(body, instance);
			
			String paramStr = "";
			for (IAstTypedExpr expr : instanceParams)
				paramStr += DumpAST.dumpString(expr)+ " ";
			System.out.println("After expanding generic of " + symbol + " for type " + bodyType + " with " + paramStr + " as  " + instanceSymbol + ":");
			DumpAST dump = new DumpAST(System.out);
			instance.accept(dump);
			
		}
		return instanceSymbol;
	}
	private ISymbol updateInstanceBody(TypeEngine typeEngine,
			IAstTypedExpr body, IAstTypedExpr instance) {
		ISymbol instanceSymbol;
		
		ISymbol symbol = getSymbol();
		instanceSymbol = symbol.getScope().addTemporary(symbol.getName());
		instanceSymbol.setType(instance.getType());
		
		// replace self-refs to symbol
		ISymbol theSymbol = symbol;
		if (instance instanceof IAstDataType) {
			theSymbol = ((IAstDataType) ((IAstDataType) body).getScope().getOwner()).getTypeName();
			((IAstDataType) instance).setTypeName(instanceSymbol);
		}

		AstNode.replaceSymbols(typeEngine, instance, theSymbol.getScope(), Collections.singletonMap(theSymbol.getNumber(), instanceSymbol));
		AstNode.replaceTypesInTree(typeEngine, instance, Collections.singletonMap(body.getType(), instance.getType()));
		instanceSymbol.setDefinition(instance);
		
		if (instance instanceof IAstDataType) {
			((IAstDataType) instance).setTypeName(instanceSymbol);
		}
		return instanceSymbol;
	}
}
