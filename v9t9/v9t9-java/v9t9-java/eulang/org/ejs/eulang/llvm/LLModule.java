/**
 * 
 */
package org.ejs.eulang.llvm;

import java.util.*;

import org.ejs.eulang.ITarget;
import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.impl.AstName;
import org.ejs.eulang.llvm.directives.LLBaseDirective;
import org.ejs.eulang.llvm.directives.LLDeclareDirective;
import org.ejs.eulang.llvm.directives.LLDefineDirective;
import org.ejs.eulang.llvm.directives.LLGlobalDirective;
import org.ejs.eulang.llvm.directives.LLTypeDirective;
import org.ejs.eulang.llvm.instrs.*;
import org.ejs.eulang.llvm.ops.*;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;
import org.ejs.eulang.symbols.LocalScope;
import org.ejs.eulang.symbols.ModuleScope;
import org.ejs.eulang.types.BasicType;
import org.ejs.eulang.types.LLAggregateType;
import org.ejs.eulang.types.LLCodeType;
import org.ejs.eulang.types.LLDataType;
import org.ejs.eulang.types.LLInstanceType;
import org.ejs.eulang.types.LLSymbolType;
import org.ejs.eulang.types.LLType;

/**
 * @author ejs
 *
 */
public class LLModule {

	List<LLBaseDirective> directives;
	List<LLBaseDirective> externDirectives;
	private final IScope globalScope;
	
	private IScope typeScope;

	private IScope moduleScope;
	
	private Map<LLType, ISymbol> emittedTypes = new HashMap<LLType, ISymbol>();
	private final TypeEngine typeEngine;
	private final ITarget target;
	private LLDefineDirective staticInit;
	
	/**
	 * 
	 */
	public LLModule(TypeEngine typeEngine, ITarget target, IScope globalScope) {
		this.typeEngine = typeEngine;
		this.target = target;
		this.globalScope = globalScope;
		directives = new ArrayList<LLBaseDirective>();
		externDirectives = new ArrayList<LLBaseDirective>();
		
		moduleScope = new ModuleScope(globalScope);
		typeScope = new LocalScope(moduleScope);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LLBaseDirective d : externDirectives) {
			sb.append(d);
			sb.append('\n');
		}
		sb.append('\n');
		for (LLBaseDirective d : directives) {
			sb.append(d);
			sb.append('\n');
		}
		return sb.toString();
	}
	
	/**
	 * @return the directives
	 */
	public List<LLBaseDirective> getDirectives() {
		return directives;
	}
	/**
	 * @return the externDirectives
	 */
	public List<LLBaseDirective> getExternDirectives() {
		return externDirectives;
	}
	/**
	 * @return the emittedTypes
	 */
	public Map<LLType, ISymbol> getEmittedTypes() {
		return emittedTypes;
	}
	/**
	 * @return the globalScope
	 */
	public IScope getGlobalScope() {
		return globalScope;
	}
	/**
	 * @return the moduleScope
	 */
	public IScope getModuleScope() {
		return moduleScope;
	}
	
	/**
	 * @return the typeScope
	 */
	public IScope getTypeScope() {
		return typeScope;
	}

	/**
	 * Find or create the symbol that uniquely references this expression
	 * @param astSymbol the name of the symbol in the AST 
	 * @param expr the concrete body for the symbol
	 * @return unique symbol
	 */
	public ISymbol getModuleSymbol(ISymbol astSymbol, LLType type) {
		String symName = constructModuleSymName(astSymbol.getScope(), astSymbol.getName(), type);
		ISymbol modSymbol = moduleScope.get(symName);
		if (modSymbol == null) {
			modSymbol = moduleScope.add(symName, false);
			modSymbol.setVisibility(astSymbol.getVisibility());
			modSymbol.setType(type);
		}
		return modSymbol;
	}

	private String constructModuleSymName(IScope scope, String name, LLType type) {
		StringBuilder sb = new StringBuilder();
		
		// put a unique scope
		sb.append(scope.getUniqueName());
		
		// and the name
		sb.append(name);
		
		// and the exact type
		sb.append("._.").append(type.getSymbolicName());
		
		String symName = sb.toString();
		return symName;
	}

	/**
	 * @param llTargetDataTypeDirective
	 */
	public void add(LLBaseDirective directive) {
		directives.add(directive);
	}
	public void addModuleDirective(LLBaseDirective directive) {
		externDirectives.add(directive);
	}

	public ISymbol addExtern(String name, LLCodeType codeType,
			LLLinkage linkage, LLVisibility visibility,
			String cconv, LLAttrType retType,
			LLArgAttrType[] argTypes, LLFuncAttrs funcAttrs, String gc) {
		name = constructModuleSymName(moduleScope, name, codeType);
		
		ISymbol modSymbol = globalScope.get(name);
		if (modSymbol == null) {
			modSymbol = globalScope.add(new AstName(name));
			modSymbol.setType(codeType);
			
			emitTypes(codeType);
			
			externDirectives.add(new LLDeclareDirective(modSymbol, linkage, visibility, cconv, 
					retType, argTypes, funcAttrs, gc));
		}
		return modSymbol;
	}

	/**
	 * @param rEFPTR
	 * @return
	 */
	public boolean addExternType(LLType type) {
		if (type == null|| type.getName() == null || type.getBasicType() == BasicType.VOID) return false;
		if (type instanceof LLInstanceType || type instanceof LLSymbolType)
			return false;
		
		if (emittedTypes.containsKey(type))
			return false;
		String typeSymbolName = type.getSymbolicName();
		ISymbol typeSymbol = globalScope.get(typeSymbolName);
		if (typeSymbol == null) {
			//typeSymbol = globalScope.add(new LocalSymbol(globalScope.nextId(), new AstName(type.getName()), null));
			typeSymbol = globalScope.add(ISymbol.Visibility.LOCAL, new AstName(typeSymbolName));
			typeSymbol.setType(type);
			externDirectives.add(new LLTypeDirective(typeSymbol, type));
		} else if (typeSymbol.getVisibility() != ISymbol.Visibility.LOCAL) {
			typeSymbol = moduleScope.get(typeSymbolName);
			if (typeSymbol != null) {
				if (typeSymbol.getType().isMoreComplete(type))
					return false;
				moduleScope.remove(typeSymbol);
			}
			typeSymbol = moduleScope.add(ISymbol.Visibility.LOCAL, new AstName(typeSymbolName));
			typeSymbol.setType(type);
			externDirectives.add(new LLTypeDirective(typeSymbol, type));
		}
		emittedTypes.put(type, typeSymbol);
		return true;
	}

	/**
	 * @return
	 */
	public int getSymbolCount() {
		return moduleScope.getSymbols().length;
	}

	/**
	 * @param type
	 */
	public boolean emitTypes(LLType type) {
		if (type == null)
			return false;
		type = typeEngine.getRealType(type);
		if (emittedTypes.containsKey(type))
			return false;
		if (!addExternType(type))
			return false;
		boolean changed = false;
		if (type instanceof LLAggregateType) {
			for (LLType agg : ((LLAggregateType) type).getTypes())
				changed |= emitTypes(agg);
		}
		changed |= emitTypes(type.getSubType());
		return changed;
	};
	
	public LLBaseDirective lookup(ISymbol symbol) {
		for (LLBaseDirective dir : directives) {
			if (dir instanceof LLDeclareDirective && ((LLDeclareDirective) dir).getSymbol().equals(symbol))
				return dir;
			if (dir instanceof LLDefineDirective && ((LLDefineDirective) dir).getSymbol().equals(symbol))
				return dir;
		}
		return null;
	}

	/**
	 * @return
	 */
	public ITarget getTarget() {
		return target;
	}

	/**
	 * @return
	 */
	public ILLCodeTarget getStaticInitTarget(LLVMGenerator gen) {
		if (staticInit == null) {
			ISymbol initSym = moduleScope.add(".global_ctors", false);
			LLCodeType codeType = typeEngine.getCodeType(typeEngine.VOID, new LLType[0]);
			initSym.setType(typeEngine.getPointerType(codeType));
			emitTypes(initSym.getType());
			
			ISymbol modInitSym = getModuleSymbol(initSym, initSym.getType());
			
			// %0 = type { i32, void ()* }
			// @llvm.global_ctors = appending global [1 x %0] [%0 { i32 65535, void ()* @_GLOBAL__I__Z9factoriali }] ; <[1 x %0]*> [#uses=0]

			LLDataType aggType = typeEngine.getDataType(moduleScope.add(".global_ctor_entry", false), 
					Collections.singletonList(initSym.getType()));
			aggType.getSymbol().setType(aggType);
			
			LLStructOp entry = new LLStructOp(aggType, new LLSymbolOp(modInitSym));
			LLArrayOp array = new LLArrayOp(typeEngine.getArrayType(aggType, 1, null), entry);
			
			emitTypes(array.getType());
			
			ISymbol llvmGlobalCtors = moduleScope.add("llvm.global_ctors", false);
			llvmGlobalCtors.setType(array.getType());
			LLGlobalDirective ctorAdd = new LLGlobalDirective(llvmGlobalCtors, LLLinkage.APPENDING, array);
			this.directives.add(ctorAdd);
			
			// make a new module-wide init func 
			IScope staticLocalScope = new LocalScope(initSym.getScope());
			staticInit = LLDefineDirective.create(gen, target, this, staticLocalScope, 
					modInitSym, codeType, null);
			this.directives.add(staticInit);
			
			staticInit.addBlock(staticLocalScope.add(".entry", false));
		}
		return staticInit;
	}

	/**
	 * @return
	 */
	public boolean hasStaticInit() {
		return staticInit != null;
	}

	public void accept(ILLCodeVisitor visitor) {
		if (visitor.enterModule(this)) {
			for (LLBaseDirective dir : getDirectives()) {
				dir.accept(visitor);
			}
		}
		visitor.exitModule(this);
	}

	/**
	 * @param sym
	 * @return
	 */
	public LLDefineDirective getDefineDirective(ISymbol sym) {
		for (LLBaseDirective dir : getDirectives()) {
			if (dir instanceof LLDefineDirective && ((LLDefineDirective) dir).getSymbol().equals(sym))
				return (LLDefineDirective) dir;
		}
		return null;
	}

	/**
	 * 
	 */
	public void finalizeTypes() {
		boolean changed;
		do {
			changed = replaceTypes(this);
		} while (changed);
		
	}
	

	

	/**
	 * @param mod
	 * @param type
	 * @param real
	 * @return
	 */
	protected boolean replaceTypes(LLModule mod) {
		final boolean[] changed = { false };
		mod.accept(new LLCodeVisitor() {
			/* (non-Javadoc)
			 * @see org.ejs.eulang.llvm.LLCodeVisitor#enterModule(org.ejs.eulang.llvm.LLModule)
			 */
			@Override
			public boolean enterModule(LLModule module) {
				changed[0] |= replaceTypes(module.getGlobalScope());
				changed[0] |= replaceTypes(module.getModuleScope());

				return true;
			}
			
			/* (non-Javadoc)
			 * @see org.ejs.eulang.llvm.LLCodeVisitor#enterDirective(org.ejs.eulang.llvm.directives.LLBaseDirective)
			 */
			@Override
			public boolean enterDirective(LLBaseDirective dir) {
				if (dir instanceof LLDefineDirective) {
					changed[0] |= replaceTypes(((LLDefineDirective) dir).getScope());
					
				}
				return true;
			}
			/* (non-Javadoc)
			 * @see org.ejs.eulang.llvm.LLCodeVisitor#enterInstr(org.ejs.eulang.llvm.LLBlock, org.ejs.eulang.llvm.instrs.LLInstr)
			 */
			@Override
			public boolean enterInstr(LLBlock block, LLInstr instr) {
				if (instr instanceof LLTypedInstr) {
					//System.out.println(instr);
					LLType newType = realizeType(((LLTypedInstr) instr).getType());
					if (newType != ((LLTypedInstr) instr).getType()) {
						((LLTypedInstr) instr).setType(newType);
						changed[0] = true;
					}
				}
				return true;
			}
			/* (non-Javadoc)
			 * @see org.ejs.eulang.llvm.LLCodeVisitor#enterOperand(org.ejs.eulang.llvm.instrs.LLInstr, int, org.ejs.eulang.llvm.ops.LLOperand)
			 */
			@Override
			public boolean enterOperand(LLInstr instr, int num,
					LLOperand operand) {
				//System.out.println(instr + " : " + operand);
				LLType newType = realizeType(operand.getType());
				if (newType != operand.getType()) {
					operand.setType(newType);
					changed[0] = true;
				}
				return true;
			}
		});
		
		
		return changed[0];
	}

	/**
	 * @param globalScope
	 * @return
	 */
	protected boolean replaceTypes(IScope scope) {
		boolean changed = false;
		for (ISymbol sym : scope) {
			LLType newType = realizeType(sym.getType());
			if (newType != null && newType != sym.getType()) {
				sym.setType(newType);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * @param type
	 * @return
	 */
	protected LLType realizeType(LLType type) {
		if(type == null)
			return null;
		if (type instanceof LLSymbolType) {
			LLType realType = typeEngine.getRealType(type);
			if (realType != null && realType != type) {
				return realType;
			}
		}
		LLType[] subs = type.getTypes();
		LLType[] origSubs = Arrays.copyOf(subs, subs.length);
		LLType[] newSubs = null;
		int idx = 0;
		for (LLType sub : origSubs) {
			LLType newSub = realizeType(sub);
			if (newSub != sub && newSub != null) {
				if (newSubs == null)
					newSubs = Arrays.copyOf(origSubs, origSubs.length);
				newSubs[idx] = newSub;
			}
			idx++;
		}
		if (newSubs != null) {
			type = type.updateTypes(typeEngine, newSubs);
		}
		return type;
	}

}