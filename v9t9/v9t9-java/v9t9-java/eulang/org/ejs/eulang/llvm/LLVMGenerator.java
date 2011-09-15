/**
 * 
 */
package org.ejs.eulang.llvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.ejs.coffee.core.utils.Pair;
import org.ejs.eulang.IBinaryOperation;
import org.ejs.eulang.IOperation;
import org.ejs.eulang.ISourceRef;
import org.ejs.eulang.ITarget;
import org.ejs.eulang.IUnaryOperation;
import org.ejs.eulang.Message;
import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.TypeEngine.Alignment;
import org.ejs.eulang.ast.*;
import org.ejs.eulang.ast.IAttrs;
import org.ejs.eulang.ast.impl.AstSymbolExpr;
import org.ejs.eulang.ast.impl.AstTypedNode;
import org.ejs.eulang.llvm.directives.LLConstantDirective;
import org.ejs.eulang.llvm.directives.LLDefineDirective;
import org.ejs.eulang.llvm.directives.LLGlobalDirective;
import org.ejs.eulang.llvm.directives.LLTargetDataTypeDirective;
import org.ejs.eulang.llvm.directives.LLTargetTripleDirective;
import org.ejs.eulang.llvm.instrs.ECast;
import org.ejs.eulang.llvm.instrs.LLAllocaInstr;
import org.ejs.eulang.llvm.instrs.LLBinaryInstr;
import org.ejs.eulang.llvm.instrs.LLBranchInstr;
import org.ejs.eulang.llvm.instrs.LLCallInstr;
import org.ejs.eulang.llvm.instrs.LLCastInstr;
import org.ejs.eulang.llvm.instrs.LLCompareInstr;
import org.ejs.eulang.llvm.instrs.LLExtractValueInstr;
import org.ejs.eulang.llvm.instrs.LLGetElementPtrInstr;
import org.ejs.eulang.llvm.instrs.LLInsertValueInstr;
import org.ejs.eulang.llvm.instrs.LLLoadInstr;
import org.ejs.eulang.llvm.instrs.LLRetInstr;
import org.ejs.eulang.llvm.instrs.LLStoreInstr;
import org.ejs.eulang.llvm.instrs.LLUnaryInstr;
import org.ejs.eulang.llvm.instrs.LLUncondBranchInstr;
import org.ejs.eulang.llvm.ops.LLArrayOp;
import org.ejs.eulang.llvm.ops.LLCastOp;
import org.ejs.eulang.llvm.ops.LLConstOp;
import org.ejs.eulang.llvm.ops.LLNullOp;
import org.ejs.eulang.llvm.ops.LLOperand;
import org.ejs.eulang.llvm.ops.LLStringLitOp;
import org.ejs.eulang.llvm.ops.LLStructOp;
import org.ejs.eulang.llvm.ops.LLSymbolOp;
import org.ejs.eulang.llvm.ops.LLTempOp;
import org.ejs.eulang.llvm.ops.LLUndefOp;
import org.ejs.eulang.llvm.ops.LLVariableOp;
import org.ejs.eulang.llvm.ops.LLZeroInitOp;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;
import org.ejs.eulang.symbols.LocalScope;
import org.ejs.eulang.types.BaseLLField;
import org.ejs.eulang.types.BasicType;
import org.ejs.eulang.types.LLAggregateType;
import org.ejs.eulang.types.LLArrayType;
import org.ejs.eulang.types.LLCodeType;
import org.ejs.eulang.types.LLDataType;
import org.ejs.eulang.types.LLInstanceField;
import org.ejs.eulang.types.LLPointerType;
import org.ejs.eulang.types.LLStaticField;
import org.ejs.eulang.types.LLSymbolType;
import org.ejs.eulang.types.LLTupleType;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.LLVoidType;
import org.ejs.eulang.types.TypeException;
import org.ejs.eulang.types.TypeInference;

/**
 * Generate LLVM instructions
 * 
 * @author ejs
 * 
 */
public class LLVMGenerator {

	private List<Message> messages;
	private final TypeEngine typeEngine;
	private final ITarget target;
	private LLModule ll;
	private ILLCodeTarget currentTarget;
	private LLVariableStorage varStorage;

	public LLVMGenerator(ITarget target) {
		this.typeEngine = target.getTypeEngine();
		this.target = target;

		messages = new ArrayList<Message>();
		varStorage = new LLVariableStorage();
	}

	private Map<String, String[]> fileText = new HashMap<String, String[]>();

	protected String getSource(ISourceRef ref) {
		if (ref == null || ref.getFile() == null)
			return "";

		String[] fileC = fileText.get(ref.getFile());
		if (fileC == null) {
			try {
				File file = new File(ref.getFile());
				FileInputStream fis = new FileInputStream(file);
				byte[] text = new byte[(int) file.length()];
				fis.read(text);
				String fileT = new String(text);
				fileText.put(ref.getFile(), fileT.split("\n"));
			} catch (IOException e) {
				return "";
			}
		}

		int offset = Math.max(1, ref.getLine());
		return fileC[offset - 1].substring(ref.getColumn());
	}

	/**
	 * @return the messages
	 */
	public List<Message> getMessages() {
		return messages;
	}

	public String getUnoptimizedText() {
		return ll.toString();
	}

	/**
	 * @param stmt
	 */
	private void ensureTypes(IAstNode node) throws ASTException {
		if (node instanceof IAstDefineStmt) {
			return;
		} else if (node instanceof IAstSymbolExpr) {
			// don't get stuck on definition and don't recurse up
			IAstSymbolExpr symExpr = (IAstSymbolExpr) node;
			if (symExpr.getDefinition() != null) {
				IAstTypedExpr instance = symExpr.getInstance();
				if (instance == null)
					throw new ASTException(node,
							"could not find an instance for "
									+ symExpr.getSymbol().getName()
									+ "; add some type specifications");

				IAstNode recursionCheck = instance;
				while (recursionCheck != null) {
					if (recursionCheck == instance)
						break;
					recursionCheck = recursionCheck.getParent();
				}
				if (recursionCheck == null)
					ensureTypes(instance);
				return;
			}
		}
		if (node instanceof IAstTypedNode) {
			IAstTypedNode typed = (IAstTypedNode) node;
			if (typed.getType() == null || !typed.getType().isComplete()) {
				throw new ASTException(node,
						"incomplete type information; add some specifications");
			}
			ll.emitTypes(typed.getType());
		}
		for (IAstNode kid : node.getChildren()) {
			ensureTypes(kid);
		}

	}

	public void generate(IAstModule module) {
		for (Map.Entry<String, String> nfEnt : module.getNonFileText()
				.entrySet())
			fileText.put(nfEnt.getKey(), nfEnt.getValue().split("\n"));

		this.ll = new LLModule(typeEngine, target, module.getOwnerScope());

		currentTarget = null;

		ll.addModuleDirective(new LLTargetDataTypeDirective(typeEngine));
		ll.addModuleDirective(new LLTargetTripleDirective(target));

		generateGlobalStmtList(module);
		
		if (ll.hasStaticInit()) {
			ILLCodeTarget target = ll.getStaticInitTarget(this);
			target.emit(new LLRetInstr(typeEngine.VOID));
		}
		
		ll.finalizeTypes();
	}

	/**
	 * @param stmtScope
	 */
	private void generateGlobalStmtList(IAstStmtScope stmtScope) {
		for (IAstStmt stmt : stmtScope.stmts().list()) {
			// TODO: rules for module scopes, exports, visibility... right now
			// we export everything
			try {
				if (stmt instanceof IAstAllocStmt)
					generateGlobalAlloc((IAstAllocStmt) stmt);
				else if (stmt instanceof IAstDefineStmt)
					generateGlobalDefine((IAstDefineStmt) stmt);
				else if (stmt instanceof IAstTypedExpr) 
					generateGlobalInit((IAstTypedExpr) stmt);
				else
					unhandled(stmt);
			} catch (ASTException e) {
				recordError(e);
			}
		}

	}

	/**
	 * @param stmt
	 * @throws ASTException 
	 */
	private void generateGlobalInit(final IAstTypedExpr stmt) throws ASTException {
		generateStaticInit(new ICodeEmitter() {
			
			@Override
			public void emit() throws ASTException {
				generateTypedExprCore(stmt);
			}
		});
	}

	public void recordError(ASTException e) {
		messages.add(new Message(e.getNode() != null ? e.getNode().getSourceRef() : null, e.getMessage()));
	}

	public void unhandled(IAstNode node) throws ASTException {
		if (node != null)
			throw new ASTException(node, "unhandled generating: "
					+ node.toString());
		else
			throw new ASTException(null, "unhandled generating node");
	}

	interface ICodeEmitter {
		void emit() throws ASTException;
	}
	
	/**
	 * {@link http://www.llvm.org/docs/LangRef.html#globalvars}
	 * 
	 * @param stmt
	 * @throws ASTException
	 */
	private void generateGlobalAlloc(IAstAllocStmt stmt) throws ASTException {
		ensureTypes(stmt);

		// TODO: simultaneous assignment?
		for (int i = 0; i < stmt.getSymbolExprs().nodeCount(); i++) {
			IAstSymbolExpr symbol = stmt.getSymbolExprs().list().get(i);
			
			LLOperand dataOp = null;
			LLType dataType = null;
			
			dataType = stmt.getType();
			ll.emitTypes(dataType);
			
			if (stmt.getExprs() != null) {
				IAstTypedExpr value = stmt.getExprs().list().get(
						stmt.getExprs().nodeCount() == 1 ? 0 : i);
				if (stmt.getType() instanceof LLCodeType) {
					assert false;
				}
				else {
					currentTarget = new StaticDataCodeTarget(this, symbol.getSymbol(), target, ll, ll.getModuleScope());
					
					value.simplify(typeEngine);
					try {
						dataOp = generateTypedExprCore(value);
						if (dataOp.getType() instanceof LLCodeType)
							dataOp.setType(typeEngine.getPointerType(dataOp.getType()));
					} catch (UnsupportedOperationException e) {
						throw new ASTException(value, "cannot initialize global data with code");
					}
					
				}
			}
			
			boolean isZero = false;
			if (dataOp == null) {
				dataOp = new LLZeroInitOp(dataType);
				isZero = true;
			}
			
			final ISymbol modSymbol = ll.getModuleSymbol(symbol.getSymbol(), dataType);
			ll.add(new LLGlobalDirective(modSymbol, null, dataOp));
			
			// add call to constructor if needed
			if (isZero && dataType instanceof LLDataType) {
				IAstDataType data = ((LLDataType) dataType).getDeclaration();
				
				if (data != null && data.needsExplicitInit()) {
					// add static init code
					final ISymbol initName = data.getInitName(typeEngine);
					
					generateStaticInit(new ICodeEmitter() {
						public void emit() throws ASTException {
							LLOperand thisOp = new LLSymbolOp(modSymbol);
							
							ISymbol modInitSym = ll.getModuleSymbol(initName, initName.getType());
							
							currentTarget.emit(new LLCallInstr(null, typeEngine.VOID, new LLSymbolOp(modInitSym), thisOp)); 
						}
					});
					
				}
			}
		}
	}

	/**
	 * @param iCodeEmitter
	 * @throws ASTException 
	 */
	private void generateStaticInit(ICodeEmitter iCodeEmitter) throws ASTException {
		ILLCodeTarget oldDefine = currentTarget;
		
		try {
			currentTarget = ll.getStaticInitTarget(this);
		
			iCodeEmitter.emit();
		} finally {
			currentTarget = oldDefine;
		}		
	}

	/**
	 * @param type
	 * @return
	 */
	private LLOperand generateDefaultInit(LLOperand thisOp, LLType type) {
		if (type instanceof LLDataType) {
			LLDataType dataType = (LLDataType) type;
			IAstDataType data = dataType.getDeclaration();
			assert data != null;
			
			ISymbol initSym = ll.getModuleSymbol(data.getInitName(typeEngine), data.getInitName(typeEngine).getType());
			
			// XXX addr
			LLOperand ptrToThis = thisOp;
			if (!(thisOp.getType()  instanceof LLPointerType) || !type.matchesExactly(thisOp.getType().getSubType())) {
				// make ptr
				if (thisOp instanceof LLVariableOp)
					ptrToThis = ((LLVariableOp) thisOp).getVariable().address(currentTarget);
			}
			
			currentTarget.emit(new LLCallInstr(null, typeEngine.VOID, new LLSymbolOp(initSym), ptrToThis));
			return null;
		}
		
		// can't predictably init this
		if (type instanceof LLArrayType && ((LLArrayType) type).getDynamicSizeExpr() != null)
			return null;
		
		return new LLZeroInitOp(type);
	}

	/**
	 * {@link http://www.llvm.org/docs/LangRef.html#functionstructure}
	 * 
	 * @param stmt
	 * @throws ASTException
	 */
	private void generateGlobalDefine(IAstDefineStmt stmt) throws ASTException {

		// only generate concrete instances
		for (IAstTypedExpr expr : stmt.bodyList()) {
			if (expr.getType() == null || expr.getType().isGeneric())
				continue;

			generateGlobalExpr(stmt, expr);
		}
		// TODO
		for (Collection<ISymbol> instanceList : stmt.bodyToInstanceMap()
				.values()) {
			for (ISymbol instance : instanceList) {
				if (!instance.getType().isGeneric())
					generateGlobalExpr(stmt, (IAstTypedExpr) instance
							.getDefinition());
			}
		}
	}

	private void generateGlobalExpr(IAstDefineStmt stmt, IAstTypedExpr expr)
			throws ASTException {
		if (expr instanceof IAstCodeExpr) {
			generateGlobalCode(stmt.getSymbol(), (IAstCodeExpr) expr);
		} else if (expr instanceof IAstLitExpr) {
			generateGlobalConstant(stmt.getSymbol(), (IAstLitExpr) expr);
		} else if (expr instanceof IAstDataType) {
			generateGlobalData(stmt.getSymbol(), (IAstDataType) expr);
		} else if (expr instanceof IAstSymbolExpr) {
			// ignore
		} else if (expr instanceof IAstType) {
			// ignore
		} else if (expr instanceof IAstStmtScope) {
			generateGlobalStmtList(((IAstStmtScope) expr));
		} else {
			unhandled(stmt);
		}
	}

	/**
	 * @param symbol
	 * @param expr
	 * @throws ASTException
	 */
	private void generateGlobalData(ISymbol symbol, IAstDataType expr)
			throws ASTException {
		ensureTypes(expr);

		// export static bits
		
		for (IAstStmt stmt : expr.stmts().list()) {
			if (stmt instanceof IAstDefineStmt)
				generateGlobalDefine((IAstDefineStmt) stmt);
		}
		for (IAstTypedNode field : expr.getStatics().list()) {
			if (field instanceof IAstAllocStmt) {
				generateGlobalAlloc((IAstAllocStmt) field);
			} else {
				assert false;
			}
		}
		
		// construct instance initializer method
		
		LLPointerType thisPtrType = typeEngine.getPointerType(expr.getType());
		ll.emitTypes(thisPtrType);
		LLCodeType dataInitFuncType = typeEngine.getCodeType(typeEngine.VOID, new LLType[] { thisPtrType });
		ISymbol initName = expr.getInitName(typeEngine);
		ISymbol initSym = ll.getModuleSymbol(initName, dataInitFuncType);
		
		LLDefineDirective initDef = (LLDefineDirective) ll.lookup(initSym);
		if (initDef == null) {

			IAstCodeExpr initCode = expr.getInitCode(typeEngine); 
			
			TypeInference typeInfer = new TypeInference(typeEngine);
			typeInfer.infer(initCode, true);
			
			Pair<LLDefineDirective, ISymbol> code = generateGlobalCode(initName, initCode);
			if (code != null)
				code.first.setVisibility(LLVisibility.PROTECTED);
		}


	}

	/**
	 * @param symbol
	 * @param expr
	 * @throws ASTException
	 */
	private void generateGlobalConstant(ISymbol symbol, IAstLitExpr expr)
			throws ASTException {
		ensureTypes(expr);

		ISymbol modSymbol = ll.getModuleSymbol(symbol, expr.getType());
		
		currentTarget = new StaticDataCodeTarget(this, symbol, target, ll, ll.getModuleScope());
		
		LLOperand op;
		expr.simplify(typeEngine);
		try {
			op = generateTypedExprCore(expr);
		} catch (UnsupportedOperationException e) {
			throw new ASTException(expr, "cannot initialize constant from definition");
		}

		ll.add(new LLConstantDirective(modSymbol, true, op));

	}

	private LLFuncAttrs getFuncAttrType(IAstCodeExpr expr) {
		List<String> attrs = new ArrayList<String>();
		attrs.add("optsize");
		if (expr.hasAttr(IAttrs.INLINE))
			attrs.add("inlinehint");
		return new LLFuncAttrs(attrs.toArray(new String[attrs.size()]));
	}

	private LLArgAttrType[] getArgAttrTypes(IAstArgDef[] argumentTypes) {
		LLArgAttrType[] attrTypes = new LLArgAttrType[argumentTypes.length];
		for (int i = 0; i < attrTypes.length; i++) {
			LLType argType = typeEngine.getRealType(argumentTypes[i].getType());
			if (argumentTypes[i].isVar()
					|| !isArgumentPassedByValue(argumentTypes[i].getType())) {
				argType = typeEngine.getPointerType(argType);
				ll.emitTypes(argType);
			}
			LLAttrs attrs = null; // new LLAttrs("noalias");
			attrTypes[i] = new LLArgAttrType(argumentTypes[i].getName(), attrs,
					argType);
		}
		return attrTypes;
	}

	private LLAttrType getRetAttrType(IAstType returnType) {
		return new LLAttrType(null, typeEngine
				.getRealType(returnType.getType()));
	}

	/**
	 * @param symbol
	 * @param expr
	 * @throws ASTException
	 */
	private Pair<LLDefineDirective, ISymbol> generateGlobalCode(ISymbol symbol, IAstCodeExpr expr)
			throws ASTException {
		if (expr.hasAttr(IAstCodeExpr.MACRO) || expr.getType().isGeneric())
			return null;

		ensureTypes(expr);

		ISymbol modSymbol = ll.getModuleSymbol(symbol, expr.getType());

		LLDefineDirective define = LLDefineDirective.create(this, target, ll, expr.getScope(), modSymbol, 
				getRetAttrType(expr.getPrototype().returnType()),
				getArgAttrTypes(expr.getPrototype().argumentTypes()),
				getFuncAttrType(expr));
		if (symbol.isTemporary())
			define.setVisibility(LLVisibility.HIDDEN);
		
		ll.add(define);

		generateCode(define, expr);
		
		return new Pair<LLDefineDirective, ISymbol>(define, modSymbol);
	}

	/**
	 * @param argVal
	 * @param ret
	 */
	private LLVariableOp makeLocalStorage(ISymbol symbol, IAstArgDef argDef,
			LLOperand argVal) {
		ILLVariable var = null;
		if (argDef != null) {
			if (argDef.isVar() || !isArgumentPassedByValue(symbol.getType()))
				var = new LLVarArgument(symbol, typeEngine);
		}
		if (var == null) {
			if (symbol.getType().getBasicType() == BasicType.REF) {
				var = new LLRefLocalVariable(symbol, typeEngine);
			} else {
				if (symbol.getType() instanceof LLArrayType)
					var = new LLLocalArrayVariable(symbol, typeEngine);
				else
					var = new LLLocalVariable(symbol, typeEngine);
			}
		}
		varStorage.registerVariable(symbol, var);

		var.allocate(currentTarget, argVal);
		return new LLVariableOp(var);
	}

	/**
	 * @param blocks
	 * @param stmts
	 * @throws ASTException
	 */
	private void generateCode(ILLCodeTarget define, IAstCodeExpr code)
			throws ASTException {
		ILLCodeTarget oldDefine = currentTarget;

		// IAstCodeExpr code = codeOrig.copy(null);

		try {
			currentTarget = define;

			IScope scope = code.getScope();
			define.addBlock(addLabel(scope, "entry"));

			// get return value
			LLType returnType = code.getPrototype().returnType().getType();

			// get address of each incoming argument, assuming it
			// will be accessed only on the frame in the best case
			for (IAstArgDef argDef : code.getPrototype().argumentTypes()) {

				ISymbol argSymbol = code.getScope().get(argDef.getName());
				LLOperand argVal = generateSymbolExpr(new AstSymbolExpr(true, argSymbol));
				/* LLVariableOp argAddrOp = */makeLocalStorage(argSymbol,
						argDef, argVal);
			}

			LLOperand ret = generateStmtList(code.stmts());

			// deallocate variables
			for (ILLVariable var : varStorage.getVariablesForScope(scope)) {
				var.deallocate(currentTarget);
			}

			if (returnType.getBasicType() != BasicType.VOID) {
				if (ret == null)
					ret = generateNil(returnType);
				LLOperand retVal = currentTarget.load(returnType, ret);
				currentTarget.emit(new LLRetInstr(returnType, retVal));
			} else {
				currentTarget.emit(new LLRetInstr(returnType));
			}
		} finally {
			currentTarget = oldDefine;
		}

	}

	/**
	 * @param stmts
	 * @throws ASTException
	 */
	private LLOperand generateStmtList(IAstNodeList<IAstStmt> stmts)
			throws ASTException {
		LLOperand result = null;
		for (IAstStmt stmt : stmts.list()) {
			// ensure we have a block
			if (false && stmt instanceof IAstLabelStmt) {
				currentTarget.addBlock(((IAstLabelStmt) stmt).getLabel()
						.getSymbol());
				continue;
			} else if (currentTarget.getCurrentBlock() == null) {
				currentTarget.addBlock(addLabel(stmts.getOwnerScope(), "block"));
			}

			result = generateStmt(stmt);

			// end of block instr
			if (false && stmt instanceof IAstGotoStmt) {
				currentTarget.setCurrentBlock(null);
			}
		}
		return result;
	}

	private LLOperand generateStmt(IAstStmt stmt) throws ASTException {

		LLOperand result = null;
		if (stmt instanceof IAstBlockStmt)
			result = generateStmtList(((IAstBlockStmt) stmt).stmts());
		else if (stmt instanceof IAstDefineStmt)
			; // ignore
		else if (stmt instanceof IAstLabelStmt)
			// no val
			generateLabelStmt((IAstLabelStmt) stmt);
		else if (stmt instanceof IAstGotoStmt)
			// no val
			generateGotoStmt((IAstGotoStmt) stmt);
		else if (stmt instanceof IAstTypedExpr)
			result = generateTypedExprCore((IAstTypedExpr) stmt);
		else
			unhandled(stmt);
		return result;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws ASTException
	 */
	private LLOperand generateBreakStmt(IAstBreakStmt stmt) throws ASTException {
		if (loopStack.isEmpty())
			throw new ASTException(stmt, "'break' is not inside a loop");
		LoopContext context = loopStack.peek();

		if (context.value == null)
			throw new ASTException(stmt, "'break' in a loop that yields void");
			
		LLOperand expr = generateTypedExpr(stmt.getExpr());
		currentTarget.store(context.value.getVariable().getSymbol().getType(),
				expr, context.value);

		currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
				context.exitLabel)));
		currentTarget.setCurrentBlock(null);

		return null;
	}

	static class LoopContext {
		private final ISymbol exitLabel;
		private ISymbol bodyLabel;
		private final ISymbol enterLabel;
		private final LLVariableOp value;
		private final IScope scope;

		private LLVariableOp inductor;

		private LLVariableOp[] inductors;
		public LLOperand stepping;

		LoopContext(IScope scope, ISymbol bodyLabel, ISymbol enterLabel,
				ISymbol exitLabel, LLVariableOp loopVal) {
			this.scope = scope;
			this.bodyLabel = bodyLabel;
			this.enterLabel = enterLabel;
			this.exitLabel = exitLabel;
			this.value = loopVal;
		}
	}

	private Stack<LoopContext> loopStack = new Stack<LoopContext>();
	private String optimizedText;

	private ISymbol addLabel(IScope scope, String name) {
		ISymbol sym = scope.addTemporary(name);
		sym.setType(typeEngine.LABEL);
		return sym;
	}
	
	/**
	 * Generate a loop. This has an implicit return value which can be set by a
	 * 'break' statement
	 * 
	 * @param stmt
	 * @return
	 * @throws ASTException
	 */
	private LLOperand generateLoopStmt(IAstLoopStmt stmt) throws ASTException {

		IScope scope = stmt.getOwnerScope();

		ISymbol loopValSym = scope.addTemporary("loopValue");
		loopValSym.setType(stmt.getType());

		LLVariableOp loopVal = null;
		if (stmt.getType().getBasicType() != BasicType.VOID)
			loopVal = makeLocalStorage(loopValSym, null, generateNil(stmt));

		ISymbol loopEnter = addLabel(currentTarget.getScope(), "loopEnter");
		ISymbol loopBody = addLabel(currentTarget.getScope(), "loopBody");

		ISymbol loopExit = addLabel(currentTarget.getScope(), "loopExit");
		loopExit.setType(typeEngine.LABEL);

		LoopContext context = new LoopContext(scope, loopBody, loopEnter,
				loopExit, loopVal);
		loopStack.push(context);

		generateLoopHeader(context, stmt);

		LLOperand loopTemp = generateTypedExpr(stmt.getBody());

		if (context.value != null)
			currentTarget.store(stmt.getType(), loopTemp, context.value);

		generateLoopFooter(context, stmt);

		loopStack.pop();

		return context.value;
	}

	/**
	 * Get the value of 'nil' for a type
	 * 
	 * @param type
	 * @return
	 * @throws ASTException
	 */
	private LLOperand generateNil(IAstTypedExpr expr) throws ASTException {
		LLType type = expr.getType();
		return generateNil(type);
	}

	private LLOperand generateNil(LLType type) throws ASTException {
		if (type.getBasicType() == BasicType.BOOL
				|| type.getBasicType() == BasicType.INTEGRAL)
			return new LLConstOp(type, 0);
		else if (type.getBasicType() == BasicType.FLOATING)
			return new LLConstOp(type, 0.0);
		else if (type.getBasicType() == BasicType.POINTER)
			return new LLNullOp(type); //BitcastOp(type, new LLConstOp(typeEngine.getPointerType(typeEngine.BYTE), 0));
		else if (type.getBasicType() == BasicType.TUPLE || type.getBasicType() == BasicType.DATA || type.getBasicType() == BasicType.ARRAY)
			return new LLZeroInitOp(type);
		else
			throw new ASTException(null, "unhandled generating nil for: "
					+ type);
	}

	/**
	 * @param context
	 * @param stmt
	 * @throws ASTException
	 */
	private void generateLoopHeader(LoopContext context, IAstLoopStmt stmt)
			throws ASTException {
		if (stmt instanceof IAstRepeatExpr) {
			// get the count
			IAstRepeatExpr repeatExpr = (IAstRepeatExpr) stmt;
			LLOperand counterVal = generateTypedExpr(repeatExpr.getExpr());

			// make a var to hold it
			ISymbol counterSym = context.scope.addTemporary("counter");
			counterSym.setType(typeEngine.INT);

			context.inductor = makeLocalStorage(counterSym, null, counterVal);

			// now do the test before the body
			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
					context.enterLabel)));
			currentTarget.addBlock(context.enterLabel);

			LLType indVarType = context.inductor.getVariable().getSymbol()
					.getType();
			LLOperand current = currentTarget
					.load(indVarType, context.inductor);

			LLOperand ret = currentTarget.newTemp(typeEngine.BOOL);
			currentTarget.emit(new LLCompareInstr("icmp", "eq", ret, current,
					new LLConstOp(indVarType, 0)));
			currentTarget.emit(new LLBranchInstr(typeEngine.BOOL, ret,
					new LLSymbolOp(context.exitLabel), new LLSymbolOp(
							context.bodyLabel)));

			currentTarget.addBlock(context.bodyLabel);

		} else if (stmt instanceof IAstWhileExpr) {
			IAstWhileExpr whileExpr = (IAstWhileExpr) stmt;

			// now do the test before the body
			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
					context.enterLabel)));
			currentTarget.addBlock(context.enterLabel);

			LLOperand test = generateTypedExpr(whileExpr.getExpr());
			currentTarget.emit(new LLBranchInstr(typeEngine.BOOL, test,
					new LLSymbolOp(context.bodyLabel), new LLSymbolOp(
							context.exitLabel)));

			currentTarget.addBlock(context.bodyLabel);
		} else if (stmt instanceof IAstDoWhileExpr) {

			// jump right in
			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
					context.enterLabel)));
			currentTarget.addBlock(context.enterLabel);

			context.bodyLabel = context.enterLabel;

		} else if (stmt instanceof IAstForExpr) {
			// get the expr
			IAstForExpr forExpr = (IAstForExpr) stmt;

			generateForCountLoopHeader(context, forExpr);

		} else {
			unhandled(stmt);
		}
	}

	/**
	 * Generate a counting for loop, where we go from 0 to some limit, feeding
	 * values into one or more inductor variables
	 * 
	 * @param context
	 * @param forExpr
	 * @throws ASTException
	 */
	private void generateForCountLoopHeader(LoopContext context,
			IAstForExpr forExpr) throws ASTException {
		LLOperand iterSource = generateTypedExpr(forExpr.getExpr());

		// make a var to hold it
		ISymbol iterableSym = context.scope.addTemporary("iterable");
		iterableSym.setType(forExpr.getExpr().getType());

		boolean isDown = false;
		IAstTypedExpr by = forExpr.getByExpr();
		IAstTypedExpr castedChild = AstTypedNode.getCastedChild(by);
		if (castedChild != null) {
			by = castedChild;
		}
		if (by instanceof IAstUnaryExpr
				&& ((IAstUnaryExpr) by).getOp() == IUnaryOperation.NEG) {
			isDown = true;
		} else if (by instanceof IAstIntLitExpr
				&& ((IAstIntLitExpr) by).getValue() < 0) {
			isDown = true;
		} else if (by instanceof IAstFloatLitExpr
				&& ((IAstFloatLitExpr) by).getValue() < 0) {
			isDown = true;
		}

		context.stepping = generateTypedExpr(forExpr.getByExpr());

		// make a var for the count(s)
		context.inductors = new LLVariableOp[forExpr.getSymbolExprs()
				.nodeCount()];
		int idx = 0;
		for (IAstSymbolExpr symExpr : forExpr.getSymbolExprs().list()) {
			LLOperand init;
			if (!isDown) {
				init = new LLConstOp(symExpr.getType(), idx);
			} else {
				// vals initialized as <total>-1, <total>-2, ...
				init = currentTarget.newTemp(symExpr.getType());
				currentTarget.emit(new LLBinaryInstr("sub", init, init
						.getType(), iterSource, new LLConstOp(init.getType(),
						idx + 1)));
			}
			context.inductors[idx++] = makeLocalStorage(symExpr.getSymbol(),
					null, init);
		}

		// now do the test before the body
		currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
				context.enterLabel)));
		currentTarget.addBlock(context.enterLabel);

		LLType indVarType = context.inductors[0].getVariable().getSymbol()
				.getType();

		LLOperand ret;

		if (!isDown) {
			// ends when the first inductor is >= the limit
			LLOperand current = currentTarget.load(indVarType,
					context.inductors[0]);
			ret = currentTarget.newTemp(typeEngine.BOOL);
			currentTarget.emit(new LLCompareInstr("icmp", "uge", ret, current,
					iterSource));
		} else {
			// ends when the last inductor is < 0
			LLOperand current = currentTarget.load(indVarType,
					context.inductors[context.inductors.length - 1]);
			ret = currentTarget.newTemp(typeEngine.BOOL);
			currentTarget.emit(new LLCompareInstr("icmp", "slt", ret, current,
					generateNil(indVarType)));
		}
		currentTarget.emit(new LLBranchInstr(typeEngine.BOOL, ret,
				new LLSymbolOp(context.exitLabel), new LLSymbolOp(
						context.bodyLabel)));

		currentTarget.addBlock(context.bodyLabel);

	}

	/**
	 * @param context
	 * @param stmt
	 */
	private void generateLoopFooter(LoopContext context, IAstLoopStmt stmt)
			throws ASTException {
		if (stmt instanceof IAstRepeatExpr) {
			// at the end of the loop, decrement the counter

			LLType indVarType = context.inductor.getVariable().getSymbol()
					.getType();
			LLOperand current = currentTarget
					.load(indVarType, context.inductor);
			LLOperand varTemp = currentTarget.newTemp(indVarType);
			currentTarget
					.emit(new LLBinaryInstr(IOperation.SUB.getLLVMName(),
							varTemp, indVarType, current, new LLConstOp(
									indVarType, 1)));
			currentTarget.store(indVarType, varTemp, context.inductor);

			// and jump back

			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
					context.enterLabel)));

			currentTarget.addBlock(context.exitLabel);
		} else if (stmt instanceof IAstWhileExpr) {
			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
					context.enterLabel)));

			currentTarget.addBlock(context.exitLabel);
		} else if (stmt instanceof IAstDoWhileExpr) {
			// at end of loop, test condition and jump back if true

			LLOperand test = generateTypedExpr(((IAstDoWhileExpr) stmt)
					.getExpr());
			currentTarget.emit(new LLBranchInstr(typeEngine.BOOL, test,
					new LLSymbolOp(context.bodyLabel), new LLSymbolOp(
							context.exitLabel)));

			currentTarget.addBlock(context.exitLabel);
		} else if (stmt instanceof IAstForExpr) {
			generateForCountLoopFooter(context, (IAstForExpr) stmt);
		} else {
			unhandled(stmt);
		}

	}

	/**
	 * @param context
	 * @param stmt
	 * @throws ASTException
	 */
	private void generateForCountLoopFooter(LoopContext context,
			IAstForExpr stmt) throws ASTException {
		// at the end of the loop, increment the counters

		for (int idx = 0; idx < context.inductors.length; idx++) {
			LLType indVarType = context.inductors[idx].getVariable()
					.getSymbol().getType();
			LLOperand current = currentTarget.load(indVarType,
					context.inductors[idx]);
			LLOperand varTemp = currentTarget.newTemp(indVarType);
			currentTarget.emit(new LLBinaryInstr(IOperation.ADD.getLLVMName(),
					varTemp, indVarType, current, context.stepping));
			currentTarget.store(indVarType, varTemp, context.inductors[idx]);
		}

		// and jump back

		currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(
				context.enterLabel)));

		currentTarget.addBlock(context.exitLabel);
	}

	private void generateLabelStmt(IAstLabelStmt stmt) {
		if (currentTarget.getCurrentBlock() != null) {
			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(stmt
					.getLabel().getSymbol())));
		}
		currentTarget.addBlock(stmt.getLabel().getSymbol());
	}

	private void generateGotoStmt(IAstGotoStmt stmt) throws ASTException {
		ISymbol falseBlock = null;
		if (stmt.getExpr() != null) {
			LLOperand test = generateTypedExpr(stmt.getExpr());
			falseBlock = addLabel(stmt.getOwnerScope(), "gt");
			currentTarget.emit(new LLBranchInstr(stmt.getExpr().getType(),
					test, new LLSymbolOp(stmt.getLabel().getSymbol()),
					new LLSymbolOp(falseBlock)));
		} else {
			currentTarget.emit(new LLUncondBranchInstr(new LLSymbolOp(stmt
					.getLabel().getSymbol())));
		}
		currentTarget.setCurrentBlock(null);
		if (falseBlock != null) {
			currentTarget.addBlock(falseBlock);
		}
	}

	private LLOperand generateAssignStmt(IAstAssignStmt stmt)
			throws ASTException {

		LLOperand[] vals = new LLOperand[stmt.getSymbolExprs().nodeCount()];
		for (int i = 0; i < stmt.getSymbolExprs().nodeCount(); i++) {
			IAstTypedExpr exprValue = stmt.getExprs().list().get(
					stmt.getExprs().nodeCount() == 1 ? 0 : i);

			LLOperand value = stmt.getExpand()
					|| stmt.getExprs().nodeCount() > 1 || i == 0 ? generateTypedExpr(exprValue)
					: vals[0];
			vals[i] = value;
		}

		LLOperand first = null;
		for (int i = 0; i < stmt.getSymbolExprs().nodeCount(); i++) {
			IAstTypedExpr symbol = stmt.getSymbolExprs().list().get(i);

			LLOperand var = generateTypedExprCore(symbol);

			// %0 = load %var
			// %1 = rhs
			// %2 = add %0, %1
			// store %2, %var
			if (stmt.getOperation() != IOperation.MOV) {
				LLOperand cur = currentTarget.load(vals[i].getType(), var);
				vals[i] = ((IBinaryOperation) stmt.getOperation()).generate(
						this, currentTarget, stmt, cur, vals[i]);

			}

			currentTarget.store(vals[i].getType(), vals[i], var);

			if (i == 0)
				first = var;
		}

		return first;
	}

	private LLOperand generateLocalAllocStmt(IAstAllocStmt stmt)
			throws ASTException {

		LLOperand first = null;

		LLOperand val = null;

		for (int i = 0; i < stmt.getSymbolExprs().nodeCount(); i++) {
			IAstSymbolExpr symbol = stmt.getSymbolExprs().list().get(i);
			IAstTypedExpr exprValue = stmt.getExprs() != null ? stmt.getExprs()
					.list().get(stmt.getExprs().nodeCount() == 1 ? 0 : i)
					: null;

			LLVariableOp ret = makeLocalStorage(symbol.getSymbol(), null, null);

			if (exprValue != null) {
				LLOperand value = stmt.getExpand()
						|| stmt.getExprs().nodeCount() > 1 || val == null ? generateTypedExpr(exprValue)
						: val;
				currentTarget.store(exprValue.getType(), value, ret);
				val = value;
			} else {
				// invoke initializer
				LLOperand value = generateDefaultInit(ret, symbol.getType());
				if (value != null) {
					currentTarget.store(symbol.getType(), value, ret);
					val = value;
				}
			}

			if (i == 0)
				first = ret;
		}

		return first;
	}

	private LLOperand generateLocalAllocTupleStmt(IAstAllocTupleStmt stmt)
			throws ASTException {

		LLOperand value = generateTypedExpr(stmt.getExpr());

		IAstNodeList<IAstTypedExpr> syms = stmt.getSymbols().elements();

		for (int idx = 0; idx < syms.nodeCount(); idx++) {
			IAstTypedExpr sym = syms.list().get(idx);
			if (!(sym instanceof IAstSymbolExpr))
				throw new ASTException(sym, "can only tuple-allocate a symbol");

			LLOperand val = currentTarget.newTemp(sym.getType());
			currentTarget.emit(new LLExtractValueInstr(val, stmt.getType(),
					value, new LLConstOp(idx)));

			// add a cast if needed
			val = generateCast(sym, sym.getType(), val.getType(), val, false);

			makeLocalStorage(((IAstSymbolExpr) sym).getSymbol(), null, val);
		}
		return value;
	}

	private LLOperand generateSymbolExpr(IAstSymbolExpr symbolExpr) {
		ISymbol symbol = symbolExpr.getSymbol();
		LLType type = symbolExpr.getType();
		return generateSymbolAddr(symbol, type);
	}

	private LLOperand generateSymbolAddr(ISymbol symbol, LLType type) {
		// TODO: out-of-scope variables
		ILLVariable var;
		if (!(symbol.getScope() instanceof LocalScope)) {
			symbol = ll.getModuleSymbol(symbol, type);
			var = getGlobalVariable(symbol);
		} else
			var = varStorage.lookupVariable(symbol);
		if (var != null)
			return new LLVariableOp(var);
		return new LLSymbolOp(symbol, type);
	}

	/**
	 * @param moduleSymbol
	 * @return
	 */
	private ILLVariable getGlobalVariable(ISymbol moduleSymbol) {
		// XXX codeptr
		if (moduleSymbol.getType() instanceof LLCodeType)
			return null;
		ILLVariable var = varStorage.lookupVariable(moduleSymbol);
		if (var != null)
			return var;
		var = new LLGlobalVariable(moduleSymbol, typeEngine);
		varStorage.registerVariable(moduleSymbol, var);
		return var;
	}

	private LLOperand generateStmtListExpr(IAstStmtListExpr expr)
			throws ASTException {

		LLOperand result = null;
		for (IAstStmt stmt : expr.getStmtList().list()) {
			result = generateStmt(stmt);
		}
		return result;
	}

	private LLOperand generateExprStmt(IAstExprStmt expr) throws ASTException {
		return generateTypedExpr(expr.getExpr());
	}

	public LLOperand generateTypedExpr(IAstTypedExpr expr) throws ASTException {
		// currentTarget.emit(new
		// LLCommentInstr(getSource(expr.getSourceRef())));
		LLOperand temp = generateTypedExprCore(expr);
		if (temp == null)
			return null;
		if (!temp.isConstant() || !temp.getType().equals(expr.getType()))
			temp = currentTarget.load(expr.getType(), temp);
		return temp;
	}

	public LLOperand generateTypedExprAddr(IAstTypedExpr expr)
			throws ASTException {
		LLOperand op = generateTypedExprCore(expr);
		if (op instanceof LLVariableOp) {
			op = ((LLVariableOp) op).getVariable().address(currentTarget);
		} else {
			// unhandled(expr);
		}
		return op;
	}

	public LLOperand generateTypedExprCore(IAstTypedExpr expr)
			throws ASTException {
		LLOperand temp = null;
		if (expr instanceof IAstExprStmt)
			temp = generateExprStmt((IAstExprStmt) expr);
		else if (expr instanceof IAstStmtListExpr)
			temp = generateStmtListExpr((IAstStmtListExpr) expr);
		else if (expr instanceof IAstLitExpr)
			temp = generateLitExpr((IAstLitExpr) expr);
		else if (expr instanceof IAstDerefExpr)
			temp = generateDerefExpr((IAstDerefExpr) expr);
		else if (expr instanceof IAstAddrOfExpr)
			temp = generateAddrOfExpr((IAstAddrOfExpr) expr);
		else if (expr instanceof IAstAddrRefExpr)
			temp = generateAddrRefExpr((IAstAddrRefExpr) expr);
		else if (expr instanceof IAstFuncCallExpr)
			temp = generateFuncCallExpr((IAstFuncCallExpr) expr);
		else if (expr instanceof IAstSymbolExpr)
			temp = generateSymbolExpr((IAstSymbolExpr) expr);
		else if (expr instanceof IAstUnaryExpr)
			temp = generateUnaryExpr((IAstUnaryExpr) expr);
		else if (expr instanceof IAstBinExpr)
			temp = generateBinExpr((IAstBinExpr) expr);
		//else if (expr instanceof IAstInitIndexExpr)
		//	temp = generateIndexExpr((IAstInitIndexExpr) expr);
		else if (expr instanceof IAstFieldExpr)
			temp = generateFieldExpr((IAstFieldExpr) expr);
		else if (expr instanceof IAstCondList)
			temp = generateCondList((IAstCondList) expr);
		else if (expr instanceof IAstTupleExpr)
			temp = generateTupleExpr((IAstTupleExpr) expr);
		else if (expr instanceof IAstAssignStmt)
			temp = generateAssignStmt((IAstAssignStmt) expr);
		else if (expr instanceof IAstAssignTupleStmt)
			temp = generateAssignTupleStmt((IAstAssignTupleStmt) expr);
		else if (expr instanceof IAstAllocStmt)
			temp = generateLocalAllocStmt((IAstAllocStmt) expr);
		else if (expr instanceof IAstAllocTupleStmt)
			temp = generateLocalAllocTupleStmt((IAstAllocTupleStmt) expr);
		else if (expr instanceof IAstInitListExpr)
			temp = generateInitListExpr((IAstInitListExpr) expr);
		else if (expr instanceof IAstBlockStmt)
			temp = generateStmtList(((IAstBlockStmt) expr).stmts());
		else if (expr instanceof IAstLoopStmt)
			temp = generateLoopStmt((IAstLoopStmt) expr);
		else if (expr instanceof IAstBreakStmt)
			temp = generateBreakStmt((IAstBreakStmt) expr);
		else if (expr instanceof IAstGotoStmt)
			generateGotoStmt((IAstGotoStmt) expr);
		else if (expr instanceof IAstCodeExpr) {
			temp = generateCodeExpr((IAstCodeExpr) expr);
			
		}
		else {
			unhandled(expr);
			return null;
		}
		return temp;
	}

	/**
	 * @param expr
	 * @return
	 */
	private LLOperand generateCodeExpr(IAstCodeExpr expr) throws ASTException {
		ISymbol codeSym = currentTarget.getScope().addTemporary(currentTarget.getSymbol().getLLVMName() + "$inner");
		codeSym.setVisibility(ISymbol.Visibility.MODULE);
		
		Pair<LLDefineDirective, ISymbol> info = generateGlobalCode(codeSym, expr);
		if (info == null)
			throw new ASTException(expr, "cannot reference the address of this macro or generic code");
		
		// var/etc args change the effective type
		LLType codeType = info.first.getConvention().getActualType(typeEngine);
		
		// must refer to the function as a pointer in LLVM
		LLType symType = typeEngine.getPointerType(codeType);
		ll.emitTypes(symType);
		
		return new LLSymbolOp(info.second, symType);
		
		/*
		codeSym.setType(expr.getType());
		Pair<LLDefineDirective, ISymbol> info = generateGlobalCode(codeSym, (IAstCodeExpr) expr);
		temp = new LLSymbolOp(info.second);
		
		ISymbol initSymbol = symbol.getSymbol().getScope().add(symbol.getSymbol().getUniqueName(), true);
		//ISymbol initSymbol = symbol.getSymbol().getScope().addTemporary(symbol.getSymbol().getUniqueName());
		Pair<LLDefineDirective, ISymbol> info = generateGlobalCode(initSymbol, (IAstCodeExpr) value);
		if (info == null)
			continue;
		// var/etc args change the effective type
		dataType = info.first.getConvention().getActualType(typeEngine);
		// XXX codeptr
		dataType = typeEngine.getPointerType(dataType);
		ll.emitTypes(dataType);
		dataOp = new LLSymbolOp(info.second, dataType);
		return null;
		*/
		
	}

	/**
	 * @param expr
	 * @return
	 * @throws ASTException
	 */
	private LLOperand generateAddrOfExpr(IAstAddrOfExpr expr)
			throws ASTException {
		LLOperand op = generateTypedExprAddr(expr.getExpr());
		op = ensureAddressable(op);
		return op;
	}

	private LLOperand generateAddrRefExpr(IAstAddrRefExpr expr)
			throws ASTException {
		return generateTypedExprAddr(expr.getExpr());
	}

	/**
	 * Only point to the address holding the desired value, so load and store
	 * can work with addresses.
	 * 
	 * @param expr
	 * @return
	 * @throws ASTException
	 */
	private LLOperand generateDerefExpr(IAstDerefExpr expr) throws ASTException {
		LLOperand source = generateTypedExprAddr(expr.getExpr());
		if (source == null)
			return null;

		LLType valueType = AstTypedNode.getConcreteType(typeEngine, expr,
				source.getType());
		if (valueType.equals(expr.getType()))
			return source;

		if (expr.getType().getBasicType() == BasicType.VOID)
			return source;

		while (valueType != null
				&& !expr.getType().isCompatibleWith(valueType.getSubType())) {
			if (source.getType().getBasicType() == BasicType.REF) {
				// dereference to get the data ptr
				LLOperand addrTemp = currentTarget.newTemp(source.getType());
				source = ensureAddressable(source);
				currentTarget.emit(new LLGetElementPtrInstr(addrTemp, source.getType(), source, new LLConstOp(0),
								new LLConstOp(0)));

				// now read data ptr
				LLType valPtrType = getTypeEngine().getPointerType(
						source.getType().getSubType());
				ll.emitTypes(valPtrType);
				LLOperand addr = currentTarget.newTemp(valPtrType);
				currentTarget.emit(new LLLoadInstr(addr, valPtrType, addrTemp));

				// now read value
				LLOperand value = currentTarget.newTemp(source.getType()
						.getSubType());
				currentTarget
						.emit(new LLLoadInstr(value, value.getType(), addr));
				source = addr;
			} else if (source.getType().getBasicType() == BasicType.POINTER) {
				LLOperand ret = currentTarget.newTemp(source.getType()
						.getSubType());
				currentTarget.emit(new LLLoadInstr(ret, ret.getType(), source));
				source = ret;
			} else {
				throw new IllegalStateException();
			}
			valueType = AstTypedNode.getConcreteType(typeEngine, expr, source
					.getType());
		}

		return source;
	}

	/**
	 * Initialize data in an array or struct and return the result.
	 * 
	 * Note LLVM favors using "physical" types that are specialized for
	 * initialization, which explicitly spell out where gaps between elements
	 * exist -- which are filled with zeroinitializer operands, and must be
	 * accounted for in the type. The #load() code will bitcast this to the
	 * actual logical type.
	 * 
	 * struct Silly { int a; char b[103]; short c; };
	 * 
	 * struct Silly example = { .a = 0, .c = 3 }, other;
	 * 
	 * struct Silly *sillyptr() { return example.a ? &example : &other; } int
	 * main(int argc, char **argv) { example.a += example.b[94]; return
	 * sillyptr(); }
	 * 
	 * ======== %0 = type { i32, [104 x i8], i16 } %struct.Silly = type { i32,
	 * [103 x i8], i16 }
	 * 
	 * @example = internal global %0 { i32 0, [104 x i8] zeroinitializer, i16 3
	 *          }, align 32 ; <%0*> [#uses=2]
	 * @other = internal global %struct.Silly zeroinitializer, align 32 ;
	 *        <%struct.Silly*> [#uses=1]
	 * 
	 *        define i32 @main(i32 %argc, i8** nocapture %argv) nounwind {
	 *        entry: %0 = load i32* getelementptr (%struct.Silly* bitcast (%0* @example
	 *        to %struct.Silly*), i64 0, i32 0), align 32 ; <i32> [#uses=1] %1 =
	 *        load i8* getelementptr (%struct.Silly* bitcast (%0* @example to
	 *        %struct.Silly*), i64 0, i32 1, i64 94), align 2 ; <i8> [#uses=1]
	 *        %2 = sext i8 %1 to i32 ; <i32> [#uses=1] %3 = add nsw i32 %2, %0 ;
	 *        <i32> [#uses=2] store i32 %3, i32* getelementptr (%struct.Silly*
	 *        bitcast (%0* @example to %struct.Silly*), i64 0, i32 0), align 32
	 *        %4 = icmp eq i32 %3, 0 ; <i1> [#uses=1] %5 = select i1 %4, i32
	 *        ptrtoint (%struct.Silly* @other to i32), i32 ptrtoint (%0* @example
	 *        to i32) ; <i32> [#uses=1] ret i32 %5 }
	 * 
	 * @param expr
	 * @return
	 */
	private LLOperand generateInitListExpr(final IAstInitListExpr expr)
			throws ASTException {

		boolean notAgg = !(expr.getType() instanceof LLAggregateType)
				&& !(expr.getType() instanceof LLArrayType);

		if (notAgg)
			return generateTypedExpr(expr.getInitExprs().getFirst().getExpr());

		if (expr.getInitExprs().nodeCount() == 0) 
			return new LLZeroInitOp(expr.getType());
		
		// constant init ops stored here, with zeroinits for nonconstant ones,
		// plus padding
		List<LLOperand> constOps = new ArrayList<LLOperand>();
		// non-const ops evaluated and stored here by position in constOps
		Map<Integer, LLOperand> nonConstOps = new LinkedHashMap<Integer, LLOperand>();
		// the op positions that are for added padding
		Set<Integer> paddingOps = new HashSet<Integer>();

		ArrayList<IAstInitNodeExpr> sortedFields = new ArrayList<IAstInitNodeExpr>(
				expr.getInitExprs().list());
		Collections.sort(sortedFields, new Comparator<IAstInitNodeExpr>() {

			@Override
			public int compare(IAstInitNodeExpr o1, IAstInitNodeExpr o2) {
				Pair<Integer, LLType> info1;
				Pair<Integer, LLType> info2;
				try {
					info1 = o1.getInitFieldInfo(expr.getType());
					info2 = o2.getInitFieldInfo(expr.getType());
					return info1.first - info2.first;
				} catch (TypeException e) {
					// ignore here
					return 0;
				}
			}

		});

		int curOffs = 0;
		int initIdx = 0;

		boolean isArray = expr.getType() instanceof LLArrayType;
		boolean needAlignment = false;		// in llvm 2.7, cannot bitcast structs anymore

		int fieldCount;
		if (expr.getType() instanceof LLAggregateType) {
			fieldCount = ((LLAggregateType) expr.getType()).getCount();
		} else {
			if (((LLArrayType) expr.getType()).getArrayCount() != 0)
				fieldCount = ((LLArrayType) expr.getType()).getArrayCount();
			else
				fieldCount = sortedFields.size();
		}

		// track the init-view types, which include explicit zeroinitializer
		// slots
		List<LLType> initFieldTypes = new ArrayList<LLType>();
		if (isArray) {
			TypeEngine.Alignment align = typeEngine.new Alignment(
					TypeEngine.Target.STRUCT);
			LLType elType = expr.getType().getSubType();
			align.alignAndAdd(elType);
			if (align.sizeof() == elType.getBits())
				initFieldTypes.add(elType);
			else {
				elType = typeEngine.getArrayType(typeEngine.BYTE, align
						.sizeof() / 8, null);
				initFieldTypes.add(elType);
			}
			ll.emitTypes(elType);
		}

		Pair<Integer, LLType> initInfo = null;
		IAstInitNodeExpr initNode = null;
		int fieldIdx;

		TypeEngine.Alignment align = typeEngine.new Alignment(
				TypeEngine.Target.STRUCT);

		for (fieldIdx = 0; fieldIdx < fieldCount
				&& initIdx < sortedFields.size(); fieldIdx++) {

			// add zero init if no init here
			if (initInfo == null) {
				initNode = sortedFields.get(initIdx);
				try {
					initInfo = initNode.getInitFieldInfo(expr.getType());
				} catch (TypeException e) {
					throw new ASTException(initNode, e.getMessage());
				}
			}

			if (initInfo.first == fieldIdx) {

				IAstTypedExpr initExpr = initNode.getExpr();
				LLOperand op;

				if (initExpr == null && initNode instanceof IAstInitListExpr) {
					op = generateInitListExpr((IAstInitListExpr) initNode);
				} else {
					op = generateTypedExpr(initExpr);

				}
				
				// don't allocate 1 bit for bools
				LLType opType = op.getType();
				if (opType.getBits() < 8)
					opType = typeEngine.BYTE; 
						
				if (needAlignment) {
					// add zero init if we skipped anything
					int gap = align.alignmentGap(opType)
							+ (align.sizeof() - curOffs);
					addZeroInitGap(gap, constOps, paddingOps, initFieldTypes,
							isArray, align);
					curOffs = align.sizeof();
				}

				if (op.isConstant()) {
					constOps.add(op);
				} else {
					/*
					if (opType instanceof LLAggregateType
							|| opType instanceof LLArrayType)
						throw new ASTException(initNode,
								"cannot initialize with variable aggregates");
					*/
					int idx = constOps.size();
					constOps.add(new LLZeroInitOp(opType));
					nonConstOps.put(idx, op);
				}

				curOffs += opType.getBits();
				align.addAtOffset(opType);
				if (!isArray)
					initFieldTypes.add(initInfo.second);

				initInfo = null;
				initIdx++;
			} else {
				// no init for this field

				LLType fieldType = expr.getType() instanceof LLAggregateType ? ((LLAggregateType) expr
						.getType()).getType(fieldIdx)
						: expr.getType().getSubType();

				align.alignAndAdd(fieldType);
				
				if (!needAlignment) {
					constOps.add(new LLZeroInitOp(fieldType));
					if (!isArray)
						initFieldTypes.add(fieldType);
				}
			}
		}

		// add zeroinits at end

		for (; fieldIdx < fieldCount; fieldIdx++) {

			LLType fieldType = expr.getType() instanceof LLAggregateType ? ((LLAggregateType) expr
					.getType()).getType(fieldIdx)
					: expr.getType().getSubType();

			align.alignAndAdd(fieldType);
			
			if (!needAlignment) {
				constOps.add(new LLZeroInitOp(fieldType));
				if (!isArray)
					initFieldTypes.add(fieldType);
			}
		}

		if (needAlignment) {
			// fill final gap
			int gap = align.sizeof() - curOffs;
			addZeroInitGap(gap, constOps, paddingOps, initFieldTypes, isArray,
					align);
		}

		LLType initType;

		if (isArray) {
			int size = ((LLArrayType) expr.getType()).getArrayCount();
			if (size == 0)
				size = constOps.size();
			initType = typeEngine.getArrayType(initFieldTypes.get(0), size,
					null);
		} else {
			ISymbol dataSym = ((LLDataType) expr.getType()).getSymbol();
			ISymbol initSym = dataSym.getScope().add(
					dataSym.getUniqueName() + "$init", true);
			initType = typeEngine.getDataType(initSym, initFieldTypes);
		}

		LLOperand initOp;
		boolean notUsingInitType = false;
		Map<Integer, Integer> origToRealMap = new HashMap<Integer, Integer>();

		if (nonConstOps.isEmpty() || initType.matchesExactly(expr.getType())) {
			if (isArray) {
				initOp = new LLArrayOp((LLArrayType) initType, constOps
						.toArray(new LLOperand[constOps.size()]));
			} else {
				initOp = new LLStructOp((LLAggregateType) initType, constOps
						.toArray(new LLOperand[constOps.size()]));
			}
		} else {
			// must explicitly copy the elements in, since we can't put the
			// non-const op
			// in a slot of a different type
			notUsingInitType = true;
			int idx = 0;
			int opPos = 0;
			initType = expr.getType();
			initOp = new LLUndefOp(initType);
			for (LLOperand op : constOps) {
				if (!paddingOps.contains(opPos)) {
					// skip stock zero-inits which are overriden by nonconsts
					if (!nonConstOps.containsKey(opPos)) {
						LLOperand temp = currentTarget.newTemp(initType);
						currentTarget.emit(new LLInsertValueInstr(temp,
								initType, initOp, op.getType(), op, idx));
						initOp = temp;
					}
					origToRealMap.put(opPos, idx);
					idx++;
				}
				opPos++;
			}

		}
		ll.emitTypes(initType);

		if (!nonConstOps.isEmpty()) {
			// inject non-consts
			for (Map.Entry<Integer, LLOperand> entry : nonConstOps.entrySet()) {
				LLOperand temp = currentTarget.newTemp(initType);
				Integer opPos = entry.getKey();
				if (notUsingInitType) {
					opPos = origToRealMap.get(opPos);
					assert opPos != null;
				}
				currentTarget.emit(new LLInsertValueInstr(temp, initType,
						initOp, entry.getValue().getType(), entry.getValue(),
						opPos));
				initOp = temp;
			}
		}

		return initOp;

	}

	private void addZeroInitGap(int gap, List<LLOperand> initOps,
			Set<Integer> paddingOps, List<LLType> initFieldTypes,
			boolean isArray, Alignment align) {
		if (gap > 0) {
			assert gap % 8 == 0;
			LLType fillType;
			if (isArray)
				fillType = initFieldTypes.get(0);
			else
				fillType = typeEngine.getArrayType(typeEngine.BYTE, gap / 8,
						null);

			if (align != null)
				align.add(gap);

			while (gap > 0) {
				paddingOps.add(initOps.size());
				initOps.add(new LLZeroInitOp(fillType));
				if (!isArray)
					initFieldTypes.add(fillType);
				gap -= fillType.getBits();
			}
		}
	}

	/*
	private LLOperand generateIndexExpr(IAstInitIndexExpr expr) throws ASTException {
		LLOperand index = generateTypedExpr(expr.getIndex());
		LLOperand array = generateTypedExprAddr(expr.getExpr());

		// point to the array
		LLType arrayType = expr.getExpr().getType();
		
		boolean isAlloca = false;
		if (arrayType instanceof LLArrayType && ((LLArrayType) arrayType).getDynamicSizeExpr() != null) {
			// convert to bare pointer
			arrayType = arrayType.getSubType();
			isAlloca = true;
		}
		
		LLType arrayPointerType = typeEngine.getPointerType(arrayType);
		// point to the element
		LLType elementType = typeEngine.getPointerType(expr.getType());

		LLTempOp elPtr = currentTarget.newTemp(elementType);
		if (!isAlloca)
			currentTarget.emit(new LLGetElementPtrInstr(elPtr, arrayPointerType,
					array, new LLConstOp(0), index));
		else
			currentTarget.emit(new LLGetElementPtrInstr(elPtr, arrayPointerType,
					array, index));

		// and load value
		// LLOperand ret = currentTarget.newTemp(expr.getType());
		// currentTarget.emit(new LLLoadInstr(ret, expr.getType(), elPtr));
		return elPtr;
	}
*/
	private LLOperand generateFieldExpr(IAstFieldExpr expr) throws ASTException {

		LLDataType dataType = (LLDataType) expr.getDataType(typeEngine);
		BaseLLField field = dataType.getField(expr.getField().getName());
		if (field == null)
			throw new ASTException(expr.getField(), "unknown field '"
					+ expr.getField().getName() + "' in '" + dataType.getName());

		// point to the element
		LLType fieldPointerType = typeEngine.getPointerType(field.getType());

		if (field instanceof LLInstanceField) {
			// dereferenced from the address of the expr

			LLOperand structAddr = generateTypedExprAddr(expr.getExpr());

			LLTempOp elPtr = currentTarget.newTemp(fieldPointerType);

			LLPointerType pointerType = typeEngine
					.getPointerType(expr.getExpr().getType());
			ll.emitTypes(pointerType);
			structAddr = ensureAddressable(structAddr);
			currentTarget.emit(new LLGetElementPtrInstr(elPtr, pointerType, structAddr,
					new LLConstOp(0), new LLConstOp(dataType
							.getFieldIndex(field))));

			// and load value
			// LLOperand ret = currentTarget.newTemp(expr.getType());
			// currentTarget.emit(new LLLoadInstr(ret, expr.getType(), elPtr));
			// return ret;
			return elPtr;
		} else if (field instanceof LLStaticField) {
			// find the symbol and use it directly
			ISymbol fieldSym = ((LLStaticField) field).getSymbol();
			if (fieldSym == null)
				throw new ASTException(expr.getField(),
						"cannot find symbol for '" + expr.getField().getName()
								+ "' in '" + dataType.getName());

			LLOperand ret = generateSymbolAddr(fieldSym, fieldPointerType);
			return ret;
		} else {
			unhandled(expr.getField());
			return null;
		}
	}

	/**
	 * A tuple is an unnamed type. We just fill in all the pieces.
	 * 
	 * @param tuple
	 * @return
	 * @throws ASTException
	 */
	private LLOperand generateTupleExpr(IAstTupleExpr tuple)
			throws ASTException {

		// ISymbol tupleSym = currentTarget.newTempSymbol();
		// tupleSym.setType(tuple.getType());
		// LLVariableOp ret = makeLocalStorage(tupleSym, false, null);

		LLOperand[] vals = new LLOperand[tuple.elements().nodeCount()];
		boolean isConst = true;
		
		for (int idx = 0; idx < tuple.elements().nodeCount(); idx++) {
			IAstTypedExpr expr = tuple.elements().list().get(idx);
			LLOperand el = generateTypedExpr(expr);
			vals[idx] = el;
			if (!el.isConstant()) {
				isConst = false;
			}
		}
		
		LLOperand ret;
		if (!isConst) {
			ret = new LLUndefOp(typeEngine.VOID);
			for (int idx = 0; idx < tuple.elements().nodeCount(); idx++) {
				
				LLOperand tmp = currentTarget.newTemp(tuple.getType());
				currentTarget.emit(new LLInsertValueInstr(tmp, tuple.getType(),
						ret, vals[idx].getType(), vals[idx], idx));
				ret = tmp;
			}
		} else {
			ret = new LLStructOp((LLAggregateType) tuple.getType(), vals);
		}
		return ret;
	}

	private LLOperand generateAssignTupleStmt(IAstAssignTupleStmt stmt)
			throws ASTException {

		LLOperand value = generateTypedExpr(stmt.getExpr());

		IAstNodeList<IAstTypedExpr> syms = stmt.getSymbols().elements();

		LLTupleType tupleType = (LLTupleType) stmt.getType();

		for (int idx = 0; idx < syms.nodeCount(); idx++) {
			IAstTypedExpr sym = syms.list().get(idx);

			LLOperand val = currentTarget.newTemp(tupleType.getType(idx));
			currentTarget.emit(new LLExtractValueInstr(val, stmt.getType(),
					value, new LLConstOp(idx)));

			// add a cast if needed
			val = generateCast(sym, sym.getType(), val.getType(), val, false);

			LLOperand var = generateTypedExprCore(sym);
			currentTarget.store(sym.getType(), val, var);
		}
		return value;
	}

	private LLOperand generateCondList(IAstCondList condList)
			throws ASTException {
		IScope scope = condList.getOwnerScope();
		ISymbol retvalSym = scope.addTemporary("cond");
		retvalSym.setType(condList.getType());
		LLOperand retval = new LLSymbolOp(retvalSym);
		currentTarget.emit(new LLAllocaInstr(retval, condList.getType()));

		// generate a series of tests
		LLBlock[] conds = new LLBlock[condList.getCondExprs().nodeCount()];

		ISymbol nextTest = null;
		int idx = 0;
		for (IAstCondExpr expr : condList.getCondExprs().list()) {
			ISymbol resultLabel;

			if (nextTest != null) {
				currentTarget.addBlock(nextTest);
			}
			if (idx + 1 < condList.getCondExprs().nodeCount()) {
				LLOperand test = generateTypedExpr(expr.getTest());
				resultLabel = addLabel(scope, "cb");
				nextTest = addLabel(scope, "ct");
				nextTest.setType(typeEngine.LABEL);
				currentTarget.emit(new LLBranchInstr(
						// expr.getTest().getType(),
						typeEngine.LLBOOL, test, new LLSymbolOp(resultLabel),
						new LLSymbolOp(nextTest)));
				currentTarget.addBlock(resultLabel);
			} else {
				// last test is always true
				resultLabel = nextTest;
			}


			LLOperand result = generateTypedExpr(expr.getExpr());

			if (result != null)
				currentTarget.emit(new LLStoreInstr(condList.getType(), result,
						retval));
			conds[idx++] = currentTarget.getPreviousBlock();
		}

		ISymbol condSetSym = addLabel(scope, "cs");
		condSetSym.setType(typeEngine.LABEL);
		currentTarget.addBlock(condSetSym);

		for (LLBlock cond : conds) {
			// null if block branched
			if (cond != null && !cond.endsWithUncondBranch())
				cond.instrs().add(
						new LLUncondBranchInstr(new LLSymbolOp(condSetSym)));
		}

		LLOperand retTemp = currentTarget.newTemp(condList.getType());
		currentTarget
				.emit(new LLLoadInstr(retTemp, condList.getType(), retval));

		return retTemp;
	}

	private LLOperand generateUnaryExpr(IAstUnaryExpr expr) throws ASTException {
		LLOperand ret;
		if (expr.getOp().getLLVMName() != null) {
			LLOperand op = generateTypedExpr(expr.getExpr());
			ret = currentTarget.newTemp(expr.getType());
			currentTarget.emit(new LLUnaryInstr(expr.getOp(), ret, expr
					.getType(), op));
		} else {
			if (expr.getOp() == IOperation.NEG) {
				// result = sub 0, val
				LLOperand op = generateTypedExpr(expr.getExpr());
				ret = currentTarget.newTemp(expr.getType());
				currentTarget.emit(new LLBinaryInstr("sub", ret,
						expr.getType(), new LLConstOp(expr.getType(), 0), op));
			} else if (expr.getOp() == IOperation.PREINC
					|| expr.getOp() == IOperation.POSTINC) {
				// get addr...
				LLOperand opAddr = generateTypedExprAddr(expr.getExpr());
				// read value
				LLOperand op = currentTarget.load(expr.getType(), opAddr);
				// increment
				LLOperand incd = currentTarget.newTemp(expr.getType());
				currentTarget.emit(new LLBinaryInstr("add", incd, expr
						.getType(), op, new LLConstOp(expr.getType(), 1)));
				// write back
				currentTarget.store(expr.getType(), incd, opAddr);
				// and yield
				ret = (expr.getOp() == IOperation.POSTINC) ? op : incd;
			} else if (expr.getOp() == IOperation.PREDEC
					|| expr.getOp() == IOperation.POSTDEC) {
				// get addr...
				LLOperand opAddr = generateTypedExprAddr(expr.getExpr());
				// read value
				LLOperand op = currentTarget.load(expr.getType(), opAddr);
				// decrement
				LLOperand decd = currentTarget.newTemp(expr.getType());
				currentTarget.emit(new LLBinaryInstr("sub", decd, expr
						.getType(), op, new LLConstOp(expr.getType(), 1)));
				// write back
				currentTarget.store(expr.getType(), decd, opAddr);
				// and yield
				ret = expr.getOp() == IOperation.POSTDEC ? op : decd;
			} else if (expr.getOp() == IOperation.CAST
					|| expr.getOp() == IOperation.UCAST) {
				LLOperand op = generateTypedExpr(expr.getExpr());
				ret = generateCast(expr, expr.getType(), expr.getExpr()
						.getType(), op, expr.getOp() == IOperation.UCAST);
			} else {
				unhandled(expr);
				ret = null;
			}
		}
		return ret;
	}

	/**
	 * Cast one value (value, w/origType) to another (type). Loads and stores
	 * automatically handle memory dereferencing, so we can ignore those casts,
	 * unless they are illegal:
	 * <p>
	 * <li>casting from value to reference (should be explicit new)
	 * <li>casting from pointer (var) to reference (another explicit new)
	 * <li>casting from reference to reference (types change, should be explicit
	 * new)
	 * <li>casting from value to pointer (should not happen)
	 * <li>casting from pointer to pointer (types chane, should not happen)
	 * <p>
	 * We handle casting from reference, pointer, etc. to value by dereferencing
	 * implicitly.
	 * @param isUnsigned 
	 */
	public LLOperand generateCast(IAstNode node, LLType type, LLType origType,
			LLOperand value, boolean isUnsigned) throws ASTException {

		if (type.getBasicType() == BasicType.VOID)
			return null;

		if (type instanceof LLSymbolType) {
			type = ((LLSymbolType) type).getRealType(typeEngine);
		}
		if (origType instanceof LLSymbolType) {
			origType = ((LLSymbolType) origType).getRealType(typeEngine);
		}

		if (origType.equals(type)) {
			// good
		} else {
			ECast cast = null;
			if ((origType.getBasicType() == BasicType.INTEGRAL || origType
					.getBasicType() == BasicType.BOOL)
					&& type.getBasicType() == BasicType.INTEGRAL
					|| type.getBasicType() == BasicType.BOOL) {
				if (origType.getBits() > type.getBits()) {
					cast = ECast.TRUNC;
				} else if (origType.getBits() < type.getBits()) {
					// TODO: signedness
					cast = isUnsigned ? ECast.ZEXT : ECast.SEXT;
				} else {
					cast = ECast.BITCAST;
				}
			} else if (origType.getBasicType() == BasicType.FLOATING
					&& type.getBasicType() == BasicType.FLOATING) {
				if (origType.getBits() > type.getBits()) {
					cast = ECast.FPTRUNC;
				} else if (origType.getBits() < type.getBits()) {
					cast = ECast.FPEXT;
				} else {
					cast = ECast.BITCAST;
				}
			} else if ((origType.getBasicType() == BasicType.INTEGRAL || origType
					.getBasicType() == BasicType.BOOL)
					&& type.getBasicType() == BasicType.FLOATING) {
				cast = isUnsigned ? ECast.UITOFP : ECast.SITOFP;
			} else if (origType.getBasicType() == BasicType.FLOATING
					&& (type.getBasicType() == BasicType.INTEGRAL || type
							.getBasicType() == BasicType.BOOL)) {
				cast = isUnsigned ? ECast.FPTOUI : ECast.FPTOSI;
			} else if (origType.getBasicType() == BasicType.VOID) {
				// not really a cast
				type = origType;
				return value;
			} else if (origType.getBasicType() == BasicType.POINTER
					&& type.getBasicType() == BasicType.INTEGRAL) {
				cast = ECast.PTRTOINT;
			} else if (origType.getBasicType() == BasicType.INTEGRAL
					&& type.getBasicType() == BasicType.POINTER) {
				cast = ECast.INTTOPTR;
			} else if (origType.getBasicType() == BasicType.POINTER
					&& type.getBasicType() == BasicType.POINTER) {
				if (origType.getLLVMName().equals(type.getLLVMName()))
					return value;
				cast = ECast.BITCAST;
			} else if (origType instanceof LLAggregateType
					&& type instanceof LLAggregateType) {
				// piecewise cast
				int count = ((LLAggregateType) origType).getCount();
				LLOperand[] pieces = new LLOperand[count];
				boolean isConst = true;
				for (int i = 0; i < count; i++) {
					LLOperand current;
					LLType theType = ((LLAggregateType) origType).getType(i);
					if (value instanceof LLStructOp) {
						current = ((LLStructOp) value).getElements()[i];
					} else {
						current = currentTarget.newTemp(theType);
						currentTarget.emit(new LLExtractValueInstr(current, origType, value, new LLConstOp(i)));
					}
					pieces[i] = generateCast(node, 
							((LLAggregateType) type).getType(i),
							theType,
							current, isUnsigned);
					isConst &= pieces[i].isConstant();
				}
				if (isConst) {
					return new LLStructOp((LLAggregateType) type, pieces);
				} else {
					LLOperand ret = new LLUndefOp(type);
					for (int i = 0; i < count; i++) {
						LLOperand tmp = currentTarget.newTemp(type);
						LLType theType = ((LLAggregateType) type).getType(i);
						currentTarget.emit(new LLInsertValueInstr(tmp, type, ret, theType, pieces[i], i));
						ret = tmp;
					}
					return ret;
				}
			} else if (origType.getBasicType() == BasicType.CODE
					&& type.getBasicType() == BasicType.POINTER) {
				// not really a cast: take the address
				if (value instanceof LLSymbolOp) {
					origType = typeEngine.getPointerType(origType);
					ll.emitTypes(origType);
				}
				if (origType.getLLVMName().equals(type.getLLVMName())) {
					value.setType(type);
					return value;
				}
				cast = ECast.BITCAST;
			} else {
				throw new ASTException(node, "cannot cast from " + origType
						+ " to " + type);
			}

			if (value.isConstant()) {
				return new LLCastOp(cast, type, value);
			}
			
			LLOperand temp = currentTarget.newTemp(type);
			value = currentTarget.load(value.getType(), value);
			currentTarget.emit(new LLCastInstr(temp, cast, origType, value,
					type));

			value = temp;
		}

		return value;
	}

	private LLOperand generateBinExpr(IAstBinExpr expr) throws ASTException {
		IBinaryOperation oper = expr.getOp();

		return oper.generate(this, currentTarget, expr);

	}

	/**
	 * @param define
	 * @param expr
	 * @return
	 */
	private LLOperand generateFuncCallExpr(IAstFuncCallExpr expr)
			throws ASTException {
		LLOperand ret = null;

		LLType realFuncType = expr.getFunction().getType();
		LLCodeType funcType;

		// XXX codeptr
		if (realFuncType instanceof LLPointerType)
			funcType = (LLCodeType) realFuncType.getSubType();
		else
			funcType = (LLCodeType) realFuncType;

		LLType[] realArgTypes = new LLType[funcType.getArgTypes().length];

		LLOperand[] ops = new LLOperand[funcType.getArgTypes().length];

		int idx = 0;
		for (IAstTypedExpr arg : expr.arguments().list()) {
			realArgTypes[idx] = funcType.getArgTypes()[idx];
			LLOperand argAddr = generateTypedExprAddr(arg);

			if (isArgumentPassedByValue(arg.getType())) {
				argAddr = currentTarget.load(arg.getType(), argAddr);
			} else {
				// point to the element
				LLType argPointerType = typeEngine.getPointerType(funcType
						.getArgTypes()[idx]);
				ll.emitTypes(argPointerType);
				realArgTypes[idx] = argPointerType;
			}
			ops[idx++] = argAddr;
		}

		LLOperand func = generateTypedExpr(expr.getFunction());
		
		if (!(funcType.getRetType() instanceof LLVoidType)) {
			ret = currentTarget.newTemp(funcType.getRetType());
		}

		currentTarget.emit(new LLCallInstr(ret, expr.getType(), func,
				ops));
		return ret;
	}

	/**
	 * @param type
	 * @return
	 */
	private boolean isArgumentPassedByValue(LLType type) {
		if (true)
			return true;
		return (type.getBasicType().getClassMask() & LLType.TYPECLASS_MEMORY) == 0
				|| type.getBasicType() == BasicType.REF
				|| type.getBasicType() == BasicType.POINTER;
	}

	private LLOperand generateLitExpr(IAstLitExpr expr) throws ASTException {
		Object object = expr.getObject();
		if (object instanceof Boolean)
			return new LLConstOp(expr.getType(),
					Boolean.TRUE.equals(object) ? 1 : 0);
		else if (object instanceof String)
			return createStringInitializer(expr);
		else if (object != null)
			return new LLConstOp(expr.getType(), (Number) object);
		else {
			// nil
			if (expr.getType().getBasicType() == BasicType.INTEGRAL || expr.getType().getBasicType() == BasicType.BOOL)
				return new LLConstOp(expr.getType(), 0);
			else if (expr.getType().getBasicType() == BasicType.FLOATING)
				return new LLConstOp(expr.getType(), 0.0);
			else if (expr.getType().getBasicType() == BasicType.POINTER)
				return generateCast(expr, expr.getType(), typeEngine.PTRDIFF,
						new LLConstOp(typeEngine.PTRDIFF, 0), false);
			else if (expr.getType().getBasicType() == BasicType.DATA || expr.getType().getBasicType() == BasicType.ARRAY)
				return new LLZeroInitOp(expr.getType());
			else
				throw new ASTException(expr, "cannot generate nil");
		}

	}

	/**
	 * Generate string literal initializer.  These are always pointers because
	 * strings have varying sizes and we don't want to introduce confusion
	 * when copying them around.
	 * @param expr
	 * @return
	 */
	private LLOperand createStringInitializer(IAstLitExpr expr) {
		LLPointerType ptrType = (LLPointerType) expr.getType();
		LLDataType dataType = (LLDataType) ptrType.getSubType();
		
		LLOperand[] els = new LLOperand[2];
		String str = expr.getLiteral();
		els[0] = new LLConstOp(dataType.getType(0), str.length());
		
		LLPointerType strLitType = typeEngine.getStringLiteralType(str.length());
		
		els[1] = new LLStringLitOp((LLArrayType) strLitType.getSubType().getType(1), str);
		LLStructOp structOp = new LLStructOp((LLAggregateType) strLitType.getSubType(), els);
		
		return ensureAddressable(structOp);
		
		/*
		LLDataType dataType = (LLDataType) expr.getType();
		
		LLOperand[] els = new LLOperand[2];
		String str = expr.getLiteral();
		els[0] = new LLConstOp(dataType.getType(0), str.length());
		
		LLDataType strLitType = typeEngine.getStringLiteralType(str.length());
		
		els[1] = new LLStringLitOp((LLArrayType) strLitType.getType(1), str);
		LLStructOp structOp = new LLStructOp(strLitType, els);
		
		return structOp;
		*/
	}

	/**
	 * @return
	 */
	public LLModule getModule() {
		return ll;
	}

	/**
	 * @return
	 */
	public TypeEngine getTypeEngine() {
		return typeEngine;
	}

	/**
	 * @param optimized
	 */
	public void setOptimizedText(String optimized) {
		this.optimizedText = optimized;
	}

	/**
	 * @return the optimizedText
	 */
	public String getOptimizedText() {
		return optimizedText;
	}

	/**
	 * If performing getelementptr on a constant, force content to 
	 * memory so getelementptr instruction works.
	 * 
	 * TODO: just generate getelementptr operand!
	 * @param op
	 * @return
	 */
	public LLOperand ensureAddressable(LLOperand op) {
		if (op != null && op.isConstant() && (op instanceof LLArrayOp || op instanceof LLStructOp)) {
			ISymbol constSymbol = ll.getModuleScope().add(".const", true);
			ISymbol modSymbol = ll.getModuleSymbol(constSymbol, op.getType());
			ll.add(new LLConstantDirective(modSymbol, true, op));
			op = new LLSymbolOp(modSymbol);
			op.setType(typeEngine.getPointerType(op.getType()));
			ll.emitTypes(op.getType());

		}
		return op;
	}
}