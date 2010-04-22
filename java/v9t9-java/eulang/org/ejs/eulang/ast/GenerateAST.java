/**
 * 
 */
package org.ejs.eulang.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.ejs.eulang.IOperation;
import org.ejs.eulang.ISourceRef;
import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.impl.AstAllocStmt;
import org.ejs.eulang.ast.impl.AstAllocTupleStmt;
import org.ejs.eulang.ast.impl.AstArgDef;
import org.ejs.eulang.ast.impl.AstArrayType;
import org.ejs.eulang.ast.impl.AstAssignStmt;
import org.ejs.eulang.ast.impl.AstAssignTupleStmt;
import org.ejs.eulang.ast.impl.AstBinExpr;
import org.ejs.eulang.ast.impl.AstBlockStmt;
import org.ejs.eulang.ast.impl.AstBoolLitExpr;
import org.ejs.eulang.ast.impl.AstBreakStmt;
import org.ejs.eulang.ast.impl.AstCodeExpr;
import org.ejs.eulang.ast.impl.AstCondExpr;
import org.ejs.eulang.ast.impl.AstCondList;
import org.ejs.eulang.ast.impl.AstDataType;
import org.ejs.eulang.ast.impl.AstDefineStmt;
import org.ejs.eulang.ast.impl.AstDoWhileExpr;
import org.ejs.eulang.ast.impl.AstExprStmt;
import org.ejs.eulang.ast.impl.AstFieldExpr;
import org.ejs.eulang.ast.impl.AstFloatLitExpr;
import org.ejs.eulang.ast.impl.AstFuncCallExpr;
import org.ejs.eulang.ast.impl.AstGotoStmt;
import org.ejs.eulang.ast.impl.AstIndexExpr;
import org.ejs.eulang.ast.impl.AstIntLitExpr;
import org.ejs.eulang.ast.impl.AstLabelStmt;
import org.ejs.eulang.ast.impl.AstModule;
import org.ejs.eulang.ast.impl.AstName;
import org.ejs.eulang.ast.impl.AstNamedType;
import org.ejs.eulang.ast.impl.AstNilLitExpr;
import org.ejs.eulang.ast.impl.AstNodeList;
import org.ejs.eulang.ast.impl.AstPrototype;
import org.ejs.eulang.ast.impl.AstRepeatExpr;
import org.ejs.eulang.ast.impl.AstReturnStmt;
import org.ejs.eulang.ast.impl.AstStatement;
import org.ejs.eulang.ast.impl.AstStmtListExpr;
import org.ejs.eulang.ast.impl.AstSymbolExpr;
import org.ejs.eulang.ast.impl.AstTupleExpr;
import org.ejs.eulang.ast.impl.AstTupleNode;
import org.ejs.eulang.ast.impl.AstType;
import org.ejs.eulang.ast.impl.AstUnaryExpr;
import org.ejs.eulang.ast.impl.AstWhileExpr;
import org.ejs.eulang.ast.impl.SourceRef;
import org.ejs.eulang.ast.impl.TokenSourceRef;
import org.ejs.eulang.parser.EulangParser;
import org.ejs.eulang.symbols.GlobalScope;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;
import org.ejs.eulang.symbols.LocalScope;
import org.ejs.eulang.symbols.ModuleScope;
import org.ejs.eulang.types.LLTupleType;
import org.ejs.eulang.types.LLType;

/**
 * Transform from the syntax tree to an AST with proper node types, type
 * information, and source references.
 * 
 * @author ejs
 * 
 */
public class GenerateAST {
	static class GenerateException extends Exception {
		private static final long serialVersionUID = -2510488670733387859L;
		private Tree tree;
		private ISourceRef ref;

		public GenerateException(Tree tree, String msg) {
			super(msg);
			this.tree = tree;
		}

		public GenerateException(ISourceRef ref, String msg) {
			super(msg);
			this.ref = ref;
		}

		public GenerateException() {
			super((String) null);
		}

		public Tree getTree() {
			return tree;
		}

		public ISourceRef getSourceRef() {
			return ref;
		}

	}

	static class TempLabelStmt extends AstStatement {

		private final IAstLabelStmt label;
		private final IAstStmt stmt;

		/**
		 * @param label
		 * @param stmt
		 */
		public TempLabelStmt(IAstLabelStmt label, IAstStmt stmt) {
			this.label = label;
			this.stmt = stmt;
		}

		/**
		 * @return the label
		 */
		public IAstLabelStmt getLabel() {
			return label;
		}

		/**
		 * @return the stmt
		 */
		public IAstStmt getStmt() {
			return stmt;
		}

		@Override
		public boolean equals(Object obj) {
			return false;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public IAstNode copy(IAstNode copyParent) {
			return null;
		}

		@Override
		public IAstNode[] getChildren() {
			return NO_CHILDREN;
		}

		@Override
		public void replaceChild(IAstNode existing, IAstNode another) {
			throw new UnsupportedOperationException();
		}
	}

	private final Map<CharStream, String> fileMap;
	private final String defaultFile;
	private IScope currentScope;
	private List<Error> errors = new ArrayList<Error>();
	private TypeEngine typeEngine;
	private GlobalScope globalScope;

	public GenerateAST(TypeEngine typeEngine, String defaultFile,
			Map<CharStream, String> fileMap) {
		this.defaultFile = defaultFile;
		this.fileMap = fileMap;
		this.globalScope = new GlobalScope();
		this.typeEngine = typeEngine;

		typeEngine.populateTypes(globalScope);

	}

	public List<Error> getErrors() {
		return errors;
	}

	protected ISourceRef getSourceRef(Tree tree) {
		if (tree instanceof CommonTree) {
			
			CommonTree cTree = (CommonTree) tree;
			Token token = cTree.getToken();
			if (token != null) {
				String file = fileMap.get(token.getChannel());
				if (file == null)
					file = defaultFile;
				//return new TokenSourceRef(file, token, tree.toStringTree()
				//		.length());
				String string = cTree.toStringTree();
				return new SourceRef(file, string.length(), tree.getLine(), 
						tree.getCharPositionInLine()+1, tree.getLine() + countNls(string), string.length()
						- string.lastIndexOf('\n'));
			}
			
		}
		String string = tree.toString();
		return new SourceRef(defaultFile, string.length(), tree.getLine(), tree
				.getCharPositionInLine() + 1,
				tree.getLine() + countNls(string), string.length()
						- string.lastIndexOf('\n'));

	}

	/**
	 * @param string
	 * @return
	 */
	private int countNls(String string) {
		int cnt = 0;
		int idx = 0;
		while (idx < string.length()) {
			if (string.charAt(idx) == '\n')
				cnt++;
			idx++;
		}
		return cnt;
	}

	/** Copy source info into node */
	protected void getSource(Tree tree, IAstNode node) {
		node.setSourceRef(getSourceRef(tree));
	}

	protected ISourceRef getEmptySourceRef(Tree tree) {
		if (tree instanceof CommonTree) {
			Token token = ((CommonTree) tree).getToken();
			if (token != null) {
				return new SourceRef(fileMap.get(token.getChannel()), 0, tree
						.getLine(), tree.getCharPositionInLine() + 1, tree
						.getLine(), tree.getCharPositionInLine() + 1);
			}
		}
		return new SourceRef(defaultFile, 0, tree.getLine(), tree
				.getCharPositionInLine() + 1, tree.getLine(), tree
				.getCharPositionInLine() + 1);

	}

	/** Copy empty source info into node */
	protected void getEmptySource(Tree tree, IAstNode node) {
		node.setSourceRef(getEmptySourceRef(tree));
	}

	/**
	 * @param tree
	 * @return
	 */
	private Iterable<Tree> iter(final Tree tree) {
		return new Iterable<Tree>() {

			@Override
			public Iterator<Tree> iterator() {
				return new Iterator<Tree>() {
					int index = 0;

					@Override
					public boolean hasNext() {
						return index < tree.getChildCount();
					}

					@Override
					public Tree next() {
						if (index >= tree.getChildCount())
							throw new NoSuchElementException();
						return tree.getChild(index++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

				};
			}
		};
	}

	/**
	 * @param construct
	 * @param class1
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T extends IAstNode> T checkConstruct(Tree tree, Class<T> klass)
			throws GenerateException {
		if (tree == null)
			throw new GenerateException(tree, "no tree to create "
					+ klass.getSimpleName());
		IAstNode node = construct(tree);
		if (node == null)
			return null;
		if (klass.isInstance(node))
			return (T) node;
		throw new GenerateException(tree, "unexpected node "
				+ node.getClass().getSimpleName() + " created, expected "
				+ klass.getSimpleName());
	}

	private void error(Tree tree, String msg) {
		Error e = new Error(getSourceRef(tree), msg);
		System.err.println(e);
		errors.add(e);
	}

	private void error(ISourceRef ref, String msg) {
		Error e = new Error(ref, msg);
		System.err.println(e);
		errors.add(e);
	}

	/**
	 * @param tree
	 */
	private void unhandled(Tree tree) throws GenerateException {
		GenerateException e = new GenerateException(tree, "Unhandled tree: "
				+ tree.toStringTree());
		System.err.println(e.getMessage());
		StackTraceElement[] stackTrace = e.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			if (stackTrace[i].getFileName().contains("GenerateAST")) {
				System.err.println(stackTrace[i].toString());
			} else {
				break;
			}
		}
		throw e;
	}

	/*
	 * public IAstSymbolExpr constructId(Tree tree) { assert
	 * tree.getChildCount() == 0;
	 * 
	 * String name = tree.getText();
	 * 
	 * IAstName nameNode = new AstName(name, currentScope); getSource(tree,
	 * nameNode);
	 * 
	 * ISymbol symbol = IAstSymbolExpr id = new AstSymbolExpr(symbol);
	 * getSource(tree, id);
	 * 
	 * return id; }
	 */

	protected IScope pushScope(IScope newScope) {
		newScope.setParent(currentScope);
		currentScope = newScope;
		return newScope;
	}

	protected IScope popScope(Tree tree) throws GenerateException {
		if (currentScope == null)
			throw new GenerateException(tree, "no current scope");
		try {
			return currentScope;
		} finally {
			currentScope = currentScope.getParent();
		}

	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	@SuppressWarnings("unchecked")
	public IAstModule constructModule(Tree tree) throws GenerateException {

		// don't push/pop, since globals are not defined at all
		currentScope = new ModuleScope(globalScope);

		IAstModule module = new AstModule(currentScope);

		IAstNodeList<IAstStmt> stmtList = checkConstruct(tree,
				IAstNodeList.class);

		module.setStmtList(stmtList);

		getSource(tree, module);

		return module;
	}

	public IAstNode construct(Tree tree) throws GenerateException {
		switch (tree.getType()) {
		case EulangParser.STMTLIST:
			return constructStmtList(tree);
		case EulangParser.LIT:
			return constructLiteral(tree);
		case EulangParser.DEFINE:
			return constructDefine(tree);
		case EulangParser.FORWARD:
			return constructForward(tree);
		case EulangParser.ALLOC:
			return constructAlloc(tree);
		case EulangParser.PROTO:
			return constructPrototype(tree);
		case EulangParser.TYPE:
			return constructTypeExpr(tree);
		case EulangParser.CODE:
		case EulangParser.MACRO:
			return constructCodeExpr(tree);
		case EulangParser.ARGDEF:
			return constructArgDef(tree);
			// case EulangParser.RETURN:
			// return constructReturn(tree);
		case EulangParser.ADD:
		case EulangParser.SUB:
		case EulangParser.MUL:
		case EulangParser.DIV:
		case EulangParser.MOD:
		case EulangParser.UDIV:
		case EulangParser.UMOD:
		case EulangParser.LSHIFT:
		case EulangParser.RSHIFT:
		case EulangParser.URSHIFT:
		case EulangParser.BITAND:
		case EulangParser.BITOR:
		case EulangParser.BITXOR:
		case EulangParser.AND:
		case EulangParser.OR:
		case EulangParser.COMPEQ:
		case EulangParser.COMPNE:
		case EulangParser.COMPLE:
		case EulangParser.COMPGE:
		case EulangParser.LESS:
		case EulangParser.GREATER:
			return constructBinaryExpr(tree);
		case EulangParser.INV:
		case EulangParser.NEG:
		case EulangParser.POSTINC:
		case EulangParser.POSTDEC:
		case EulangParser.PREINC:
		case EulangParser.PREDEC:
			return constructUnaryExpr(tree);
		case EulangParser.NOT:
			return constructLogicalNot(tree);

		case EulangParser.IDEXPR:
			return constructIdExpr(tree);
		case EulangParser.IDREF:
			return constructIdRef(tree);
		case EulangParser.ASSIGN:
			return constructAssign(tree);
		case EulangParser.EXPR:
			return construct(tree.getChild(0));

		case EulangParser.CALL:
			return constructCallOrCast(tree);
		case EulangParser.ARGLIST:
			return constructArgList(tree);

			// case EulangParser.INVOKE:
			// return constructInvoke(tree);

		case EulangParser.LABELSTMT:
			return constructLabelStmt(tree);
		case EulangParser.LABEL:
			return constructLabel(tree);

		case EulangParser.STMTEXPR:
			return constructStmtExpr(tree);
		case EulangParser.GOTO:
			return constructGotoStmt(tree);
		case EulangParser.BLOCK:
			return constructBlockStmt(tree);

		case EulangParser.CONDLIST:
			return constructCondList(tree);
		case EulangParser.CONDTEST:
			return constructCondExpr(tree);

		case EulangParser.TUPLE:
			return constructTuple(tree);

		case EulangParser.LIST:
			return constructList(tree);

		case EulangParser.INDEX:
			return constructIndex(tree);

		case EulangParser.REPEAT:
			return constructRepeat(tree);
		case EulangParser.WHILE:
			return constructWhile(tree);
		case EulangParser.DO:
			return constructDoWhile(tree);
		case EulangParser.BREAK:
			return constructBreak(tree);

		case EulangParser.DATA:
			return constructData(tree);

		default:
			unhandled(tree);
			return null;
		}

	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructForward(Tree tree) throws GenerateException {
		for (Tree id : iter(tree)) {
			String name = id.getText();

			ISymbol symbol = currentScope.get(name);
			if (symbol != null)
				throw new GenerateException(id, "redefining " + name);

			IAstName nameNode = new AstName(name, currentScope);
			getSource(id, nameNode);

			if (symbol == null) {
				symbol = currentScope.add(nameNode);
				System.out.println("Creating " + symbol);
			}
		}
		return null;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructData(Tree tree) throws GenerateException {
		pushScope(new LocalScope(currentScope));
		try {
			IAstNodeList<IAstTypedNode> fields = new AstNodeList<IAstTypedNode>(
					IAstTypedNode.class);
			IAstNodeList<IAstTypedNode> statics = new AstNodeList<IAstTypedNode>(
					IAstTypedNode.class);
			for (Tree kid : iter(tree)) {
				IAstNodeList<IAstTypedNode> theList = fields;
				if (kid.getType() == EulangParser.STATIC) {
					theList = statics;
					kid = kid.getChild(0);
				}
				IAstTypedNode item = checkConstruct(kid, IAstTypedNode.class);

				if (item instanceof IAstSymbolExpr) {
					// convert to an alloc
					item.setParent(null);
					IAstSymbolExpr symbolExpr = (IAstSymbolExpr) item;
					IAstAllocStmt alloc = new AstAllocStmt(AstNodeList
							.<IAstSymbolExpr> singletonList(
									IAstSymbolExpr.class, symbolExpr), null,
							null, false);
					item = alloc;
					symbolExpr.getSymbol().setDefinition(item);
					getSource(kid, alloc);
				}
				theList.add(item);
			}
			getSource(tree, statics);
			getSource(tree, fields);

			IAstDataType dataType = new AstDataType(fields, statics,
					currentScope);
			getSource(tree, dataType);

			return dataType;
		} finally {
			popScope(tree);
		}
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructIndex(Tree tree) throws GenerateException {
		IAstTypedExpr expr = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstTypedExpr at = checkConstruct(tree.getChild(1), IAstTypedExpr.class);
		IAstIndexExpr index = new AstIndexExpr(expr, at);
		getSource(tree, index);
		return index;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructBreak(Tree tree) throws GenerateException {
		IAstTypedExpr expr = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstBreakStmt breakStmt = new AstBreakStmt(expr);
		getSource(tree, breakStmt);
		return breakStmt;
	}

	private IAstNode constructWhile(Tree tree) throws GenerateException {
		pushScope(new LocalScope(currentScope));
		try {
			IAstTypedExpr expr = checkConstruct(tree.getChild(0),
					IAstTypedExpr.class);
			IAstTypedExpr body = checkConstruct(tree.getChild(1),
					IAstTypedExpr.class);
			IAstWhileExpr while_ = new AstWhileExpr(currentScope, expr, body);
			getSource(tree, while_);
			return while_;
		} finally {
			popScope(tree);
		}
	}

	private IAstNode constructDoWhile(Tree tree) throws GenerateException {
		pushScope(new LocalScope(currentScope));
		try {
			IAstTypedExpr body = checkConstruct(tree.getChild(0),
					IAstTypedExpr.class);
			IAstTypedExpr expr = checkConstruct(tree.getChild(1),
					IAstTypedExpr.class);
			IAstDoWhileExpr dowhile = new AstDoWhileExpr(currentScope, body,
					expr);
			getSource(tree, dowhile);
			return dowhile;
		} finally {
			popScope(tree);
		}
	}

	private IAstNode constructRepeat(Tree tree) throws GenerateException {
		pushScope(new LocalScope(currentScope));
		try {
			IAstTypedExpr expr = checkConstruct(tree.getChild(0),
					IAstTypedExpr.class);
			IAstTypedExpr body = checkConstruct(tree.getChild(1),
					IAstTypedExpr.class);
			IAstRepeatExpr repeat = new AstRepeatExpr(currentScope, expr, body);
			getSource(tree, repeat);
			return repeat;
		} finally {
			popScope(tree);
		}
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructList(Tree tree) throws GenerateException {
		IAstNodeList<IAstTypedExpr> list = new AstNodeList<IAstTypedExpr>(
				IAstTypedExpr.class);

		for (Tree kid : iter(tree)) {
			list.add(checkConstruct(kid, IAstTypedExpr.class));
		}
		getSource(tree, list);
		return list;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstTupleExpr constructTuple(Tree tree) throws GenerateException {
		IAstNodeList<IAstTypedExpr> elements = new AstNodeList<IAstTypedExpr>(
				IAstTypedExpr.class);
		for (Tree kid : iter(tree)) {
			elements.add(checkConstruct(kid, IAstTypedExpr.class));
		}
		getSource(tree, elements);
		IAstTupleExpr node = new AstTupleExpr(elements);
		getSource(tree, node);
		return node;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructCondList(Tree tree) throws GenerateException {
		IAstNodeList<IAstCondExpr> condExprList = new AstNodeList<IAstCondExpr>(
				IAstCondExpr.class);
		for (Tree kid : iter(tree)) {
			IAstCondExpr arg = checkConstruct(kid, IAstCondExpr.class);
			condExprList.add(arg);
			arg.setParent(condExprList);
		}
		getSource(tree, condExprList);
		IAstCondList condList = new AstCondList(condExprList);
		getSource(tree, condList);
		return condList;
	}

	private IAstNode constructCondExpr(Tree tree) throws GenerateException {
		assert tree.getChildCount() == 2;
		IAstTypedExpr test = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstTypedExpr expr = checkConstruct(tree.getChild(1),
				IAstTypedExpr.class);
		// flatten code blocks
		if (expr instanceof IAstCodeExpr) {
			// TODO: we need to shove the scope somewhere!
			IAstCodeExpr origCode = ((IAstCodeExpr) expr);
			origCode.stmts().setParent(null);
			expr = new AstStmtListExpr(origCode.stmts());
			expr.setSourceRef(origCode.getSourceRef());
			/*
			 * IAstNodeList<IAstTypedExpr> args = new
			 * AstNodeList<IAstTypedExpr>(); getEmptySource(tree.getChild(1),
			 * args); expr = new AstFuncCallExpr(expr, args); getSource(tree,
			 * expr);
			 */
		}
		IAstCondExpr condExpr = new AstCondExpr(test, expr);
		getSource(tree, condExpr);
		return condExpr;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	@SuppressWarnings("unchecked")
	private IAstNode constructBlockStmt(Tree tree) throws GenerateException {
		pushScope(new LocalScope(currentScope));
		try {
			IAstNodeList<IAstStmt> stmtList = checkConstruct(tree.getChild(0),
					IAstNodeList.class);

			IAstBlockStmt block = new AstBlockStmt(stmtList, currentScope);
			getSource(tree, block);
			return block;
		} finally {
			popScope(tree);
		}
	}

	private IAstNode constructGotoStmt(Tree tree) throws GenerateException {
		IAstSymbolExpr label = checkConstruct(tree.getChild(0),
				IAstSymbolExpr.class);
		label.getSymbol().setType(typeEngine.LABEL);

		IAstTypedExpr test = null;
		if (tree.getChildCount() == 2)
			test = checkConstruct(tree.getChild(1), IAstTypedExpr.class);

		IAstGotoStmt gotoStmt = new AstGotoStmt(label, test);
		getSource(tree, gotoStmt);
		return gotoStmt;
	}

	private TempLabelStmt constructLabelStmt(Tree tree)
			throws GenerateException {
		// ^(LABELSTMT ^(LABEL idRef) codeStmt)
		IAstLabelStmt label = checkConstruct(tree.getChild(0),
				IAstLabelStmt.class);
		IAstStmt stmt = checkConstruct(tree.getChild(1), IAstStmt.class);
		return new TempLabelStmt(label, stmt);
	}

	private IAstNode constructLabel(Tree tree) throws GenerateException {
		IAstSymbolExpr label = createSymbol(tree.getChild(0));
		label.setType(typeEngine.LABEL);

		IAstLabelStmt labelStmt = new AstLabelStmt(label);
		getSource(tree, labelStmt);

		label.getSymbol().setDefinition(labelStmt);

		return labelStmt;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	private IAstNode constructStmtExpr(Tree tree) throws GenerateException {
		assert tree.getChildCount() == 1;

		IAstTypedExpr expr = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstStmt stmt = new AstExprStmt(expr);
		getSource(tree, stmt);
		return stmt;
	}

	/**
	 * @param tree
	 * @return
	 */
	public IAstNode constructArgList(Tree tree) throws GenerateException {
		IAstNodeList<IAstTypedExpr> argList = new AstNodeList<IAstTypedExpr>(
				IAstTypedExpr.class);
		for (Tree kid : iter(tree)) {
			IAstTypedExpr arg = checkConstruct(kid, IAstTypedExpr.class);
			argList.add(arg);
			arg.setParent(argList);
		}
		getSource(tree, argList);
		return argList;
	}

	/**
	 * @param tree
	 * @return
	 * @throws GenerateException
	 */
	@SuppressWarnings("unchecked")
	public IAstNode constructCallOrCast(Tree tree) throws GenerateException {
		assert tree.getChildCount() == 2;

		IAstTypedExpr function = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstNodeList<IAstTypedExpr> args = checkConstruct(tree.getChild(1),
				IAstNodeList.class);

		// check for a cast
		if (args.nodeCount() == 1 && function instanceof IAstSymbolExpr) {
			ISymbol funcSym = ((IAstSymbolExpr) function).getSymbol();
			IAstNode symdef = funcSym.getDefinition();
			if (symdef instanceof IAstType
					&& ((IAstType) symdef).getType() != null) {
				IAstTypedExpr[] argNodes = args.getNodes(IAstTypedExpr.class);
				argNodes[0].setParent(null);
				IAstUnaryExpr castExpr = new AstUnaryExpr(IOperation.CAST,
						argNodes[0]);
				castExpr.setType(((IAstType) symdef).getType());
				getSource(tree, castExpr);
				return castExpr;
			}
		}

		if (function instanceof IAstSymbolExpr)
			((IAstSymbolExpr) function).setAddress(true);

		IAstFuncCallExpr funcCall = new AstFuncCallExpr(function, args);
		getSource(tree, funcCall);
		return funcCall;
	}

	public IAstNode constructAlloc(Tree tree) throws GenerateException {
		IAstType type = tree.getChildCount() > 1 ? checkConstruct(tree
				.getChild(1), IAstType.class) : null;

		if (tree.getChild(0).getType() == EulangParser.ID) {
			IAstSymbolExpr symbolExpr = createSymbol(tree.getChild(0));

			if (type != null) {
				symbolExpr.getSymbol().setType(type.getType());
			}

			IAstNodeList<IAstSymbolExpr> idlist = AstNodeList
					.<IAstSymbolExpr> singletonList(IAstSymbolExpr.class,
							symbolExpr);

			IAstNodeList<IAstTypedExpr> exprlist = null;
			if (tree.getChildCount() == 3) {
				IAstTypedExpr expr = checkConstruct(tree.getChild(2),
						IAstTypedExpr.class);
				exprlist = AstNodeList.<IAstTypedExpr> singletonList(
						IAstTypedExpr.class, expr);
			}

			IAstAllocStmt alloc = new AstAllocStmt(idlist, type, exprlist,
					false);
			getSource(tree, alloc);

			symbolExpr.getSymbol().setDefinition(alloc);

			return alloc;
		} else if (tree.getChild(0).getType() == EulangParser.LIST) {
			IAstNodeList<IAstSymbolExpr> idlist = new AstNodeList<IAstSymbolExpr>(
					IAstSymbolExpr.class);

			boolean expand = false;
			int idx = 0;

			for (Tree kid : iter(tree.getChild(idx))) {
				IAstSymbolExpr symbolExpr = createSymbol(kid);

				if (type != null) {
					symbolExpr.getSymbol().setType(type.getType());
				}
				idlist.add(symbolExpr);
			}
			getSource(tree.getChild(idx), idlist);
			idx += 2; // skip TYPE

			if (tree.getChildCount() > idx) {
				if (tree.getChild(idx).getType() == EulangParser.PLUS) {
					expand = true;
					idx++;
				}
			}

			IAstNodeList<IAstTypedExpr> exprlist = null;
			if (tree.getChildCount() > idx) {
				exprlist = new AstNodeList<IAstTypedExpr>(IAstTypedExpr.class);
				for (Tree kid : iter(tree.getChild(idx))) {
					IAstTypedExpr expr = checkConstruct(kid,
							IAstTypedExpr.class);
					exprlist.add(expr);
				}
				getSource(tree.getChild(idx), exprlist);
				idx++;
			}

			if (exprlist != null && exprlist.nodeCount() != idlist.nodeCount()
					&& exprlist.nodeCount() != 1) {
				throw new GenerateException(
						tree,
						"multi-allocation statement has incompatible number of identifiers and expressions "
								+ idlist.nodeCount()
								+ " != "
								+ exprlist.nodeCount());
			}
			if (expand && (exprlist == null || exprlist.nodeCount() != 1))
				throw new GenerateException(tree,
						"expand modifier ('+') makes no sense without a singular expression");

			IAstAllocStmt alloc = new AstAllocStmt(idlist, type, exprlist,
					expand);
			getSource(tree, alloc);

			for (IAstSymbolExpr symbolExpr : idlist.list())
				symbolExpr.getSymbol().setDefinition(alloc);

			return alloc;
		} else if (tree.getChild(0).getType() == EulangParser.TUPLE) {
			IAstTupleNode syms = constructIdTuple(tree.getChild(0));

			IAstTypedExpr expr = null;
			if (tree.getChildCount() == 3)
				expr = checkConstruct(tree.getChild(2), IAstTypedExpr.class);

			IAstAllocTupleStmt alloc = new AstAllocTupleStmt(syms, type, expr);
			getSource(tree, alloc);

			if (type != null) {
				for (IAstTypedExpr elExpr : syms.elements().list()) {
					if (!(elExpr instanceof IAstSymbolExpr))
						throw new GenerateException(tree.getChild(0), "can only tuple-allocate locals");
					IAstSymbolExpr symExpr = (IAstSymbolExpr) elExpr;
					symExpr.getSymbol().setType(type.getType());
					symExpr.getSymbol().setDefinition(alloc.getExpr());
				}
			}

			return alloc;
		} else {
			unhandled(tree);
			return null;
		}
	}

	/**
	 * @param child
	 * @return
	 * @throws GenerateException
	 */
	private IAstSymbolExpr createSymbol(Tree id) throws GenerateException {
		String name = id.getText();

		ISymbol symbol = currentScope.get(name);
		IAstNode def = symbol != null ? symbol.getDefinition() : null;
		if (symbol != null && def != null && !isMacroArg(def)) {
			throw new GenerateException(id, "redefining " + name);
		}
		IAstName nameNode = new AstName(name, currentScope);
		getSource(id, nameNode);

		if (symbol == null) {
			symbol = currentScope.add(nameNode);
			System.out.println("Creating " + symbol);
		}

		IAstSymbolExpr symbolExpr = new AstSymbolExpr(symbol);
		symbolExpr.setSourceRef(nameNode.getSourceRef());

		return symbolExpr;
	}

	/**
	 * @param definition
	 * @return
	 */
	private boolean isMacroArg(IAstNode definition) {
		return definition instanceof IAstArgDef
				&& ((IAstArgDef) definition).isMacro();
	}

	/**
	 * @param tree
	 * @return
	 */
	public IAstNode constructAssign(Tree tree) throws GenerateException {
		if (tree.getChild(0).getType() == EulangParser.IDEXPR) {
			IAstTypedExpr left = checkConstruct(tree.getChild(0),
					IAstTypedExpr.class);
			IAstTypedExpr right = checkConstruct(tree.getChild(1),
					IAstTypedExpr.class);
			IAstAssignStmt assign = new AstAssignStmt(
					AstNodeList.<IAstTypedExpr> singletonList(
							IAstTypedExpr.class, left), AstNodeList
							.<IAstTypedExpr> singletonList(IAstTypedExpr.class,
									right), false);
			getSource(tree, assign);
			return assign;
		} else if (tree.getChild(0).getType() == EulangParser.LIST) {
			IAstNodeList<IAstTypedExpr> symbols = new AstNodeList<IAstTypedExpr>(
					IAstTypedExpr.class);
			IAstNodeList<IAstTypedExpr> exprs = new AstNodeList<IAstTypedExpr>(
					IAstTypedExpr.class);

			boolean expand = false;
			int idx = 0;
			for (Tree kid : iter(tree.getChild(idx))) {
				IAstTypedExpr left = checkConstruct(kid, IAstTypedExpr.class);
				symbols.add(left);
			}
			getSource(tree.getChild(idx), symbols);
			++idx;
			if (tree.getChild(idx).getType() == EulangParser.PLUS) {
				expand = true;
				idx++;
			}
			for (Tree kid : iter(tree.getChild(idx))) {
				IAstTypedExpr right = checkConstruct(kid, IAstTypedExpr.class);
				exprs.add(right);
			}
			getSource(tree.getChild(idx), exprs);
			IAstAssignStmt assign = new AstAssignStmt(symbols, exprs, expand);
			getSource(tree, assign);
			return assign;
		} else if (tree.getChild(0).getType() == EulangParser.TUPLE) {
			IAstTupleNode left = constructIdTuple(tree.getChild(0));
			IAstTypedExpr right = checkConstruct(tree.getChild(1),
					IAstTypedExpr.class);
			IAstAssignTupleStmt assign = new AstAssignTupleStmt(left, right);
			getSource(tree, assign);
			return assign;

		} else {
			unhandled(tree);
			return null;
		}
	}

	/**
	 * @param child
	 * @return
	 * @throws GenerateException
	 */
	private IAstTupleNode constructIdTuple(Tree tree) throws GenerateException {
		IAstNodeList<IAstTypedExpr> elements = new AstNodeList<IAstTypedExpr>(
				IAstTypedExpr.class);
		for (Tree kid : iter(tree)) {
			elements.add(checkConstruct(kid, IAstTypedExpr.class));
		}
		getSource(tree, elements);
		IAstTupleNode node = new AstTupleNode(elements);
		getSource(tree, node);
		return node;
	}

	public IAstTypedExpr constructIdRef(Tree tree) throws GenerateException {
		// could have ':'s
		IScope startScope = currentScope;
		int idx = 0;
		boolean inScope = false;
		
		// go up through scope backtracks...
		while (idx < tree.getChildCount()) {
			Tree kid = tree.getChild(idx);
			if (kid.getType() == EulangParser.COLON) {
				if (startScope.getParent() == null) {
					throw new GenerateException(tree,
							"Cannot go out of module scope");
				} else {
					startScope = startScope.getParent();
				}
				inScope = true;
			} else {
				break;
			}
			idx++;
		}
		
		// find a symbol
		ISymbol symbol = null;

		if (idx < tree.getChildCount()) {
			Tree kid = tree.getChild(idx);
			if (kid.getType() == EulangParser.ID) {
				if (inScope) {
					symbol = startScope.get(kid.getText());
				} else {
					symbol = startScope.search(kid.getText());
				}
				if (symbol == null) {
					if (inScope) {
						throw new GenerateException(tree, "Cannot resolve name in scope: "
								+ tree.toStringTree());
					}
					// make forward
					symbol = startScope.add(new AstName(kid.getText()));
				}
				startScope = symbol.getScope();
			} else {
				unhandled(kid);
				return null;
			}
			idx++;
		}

		if (symbol == null) {
			throw new GenerateException(tree, "Cannot resolve symbol: "
					+ tree.toStringTree());
		}
		
		IAstSymbolExpr symExpr = new AstSymbolExpr(symbol);
		getSource(tree, symExpr);
		
		IAstTypedExpr idExpr = symExpr;
		
		// may have field references
		while (idx < tree.getChildCount()) {
			Tree kid = tree.getChild(idx);
			IAstName name = new AstName(kid.getText(), symExpr != null ? symExpr.getSymbol().getScope() : null);
			getSource(kid, name);
			idExpr = new AstFieldExpr(idExpr, name); 
			getSource(tree, idExpr);
			idx++;
		}
		return idExpr;
	}

	@SuppressWarnings("unchecked")
	public IAstTypedExpr constructIdExpr(Tree tree) throws GenerateException {
		// idref is first
		IAstTypedExpr symExpr = constructIdRef(tree.getChild(0));
		
		IAstTypedExpr idExpr = symExpr;
		
		// then, possible other fun 
		int idx = 1;
		while (idx < tree.getChildCount()) {
			Tree kid = tree.getChild(idx++);
			
			if (kid.getType() == EulangParser.INDEX) {
				IAstTypedExpr index = checkConstruct(kid.getChild(0), IAstTypedExpr.class);
				idExpr = new AstIndexExpr(idExpr, index);
				getSource(kid, index);
				getSource(kid, idExpr);
				symExpr = null;
			}
			else if (kid.getType() == EulangParser.FIELDREF) {
				IAstName name = new AstName(kid.getChild(0).getText(), null);
				getSource(kid, name);
				idExpr = new AstFieldExpr(idExpr, name); 
				getSource(tree, idExpr);
			}
			else if (kid.getType() == EulangParser.CALL) {
		
				IAstNodeList<IAstTypedExpr> args = checkConstruct(kid.getChild(0),
						IAstNodeList.class);

				// check for a cast
				if (args.nodeCount() == 1 && idExpr instanceof IAstSymbolExpr) {
					ISymbol funcSym = ((IAstSymbolExpr) idExpr).getSymbol();
					IAstNode symdef = funcSym.getDefinition();
					if (symdef instanceof IAstType
							&& ((IAstType) symdef).getType() != null) {
						IAstTypedExpr[] argNodes = args.getNodes(IAstTypedExpr.class);
						argNodes[0].setParent(null);
						IAstUnaryExpr castExpr = new AstUnaryExpr(IOperation.CAST,
								argNodes[0]);
						castExpr.setType(((IAstType) symdef).getType());
						getSource(tree, castExpr);
						idExpr = castExpr;
						continue;
					}
				}

				//if (function instanceof IAstSymbolExpr)
				//	((IAstSymbolExpr) function).setAddress(true);

				IAstFuncCallExpr funcCall = new AstFuncCallExpr(idExpr, args);
				getSource(tree, funcCall);
				idExpr = funcCall;
			}
			else
				unhandled(kid);
		}
		return idExpr;
	}

	public IAstBinExpr constructBinaryExpr(Tree tree) throws GenerateException {
		assert (tree.getChildCount() == 2);
		IAstTypedExpr left = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstTypedExpr right = checkConstruct(tree.getChild(1),
				IAstTypedExpr.class);
		IAstBinExpr binop = null;

		switch (tree.getType()) {
		case EulangParser.ADD:
			binop = new AstBinExpr(IOperation.ADD, left, right);
			break;
		case EulangParser.SUB:
			binop = new AstBinExpr(IOperation.SUB, left, right);
			break;
		case EulangParser.MUL:
			binop = new AstBinExpr(IOperation.MUL, left, right);
			break;
		case EulangParser.DIV:
			binop = new AstBinExpr(IOperation.DIV, left, right);
			break;
		case EulangParser.UDIV:
			binop = new AstBinExpr(IOperation.UDIV, left, right);
			break;
		case EulangParser.MOD:
			binop = new AstBinExpr(IOperation.MOD, left, right);
			break;
		case EulangParser.UMOD:
			binop = new AstBinExpr(IOperation.UMOD, left, right);
			break;

		case EulangParser.URSHIFT:
			binop = new AstBinExpr(IOperation.SHR, left, right);
			break;
		case EulangParser.RSHIFT:
			binop = new AstBinExpr(IOperation.SAR, left, right);
			break;
		case EulangParser.LSHIFT:
			binop = new AstBinExpr(IOperation.SHL, left, right);
			break;

		case EulangParser.COMPEQ:
			binop = new AstBinExpr(IOperation.COMPEQ, left, right);
			break;
		case EulangParser.COMPNE:
			binop = new AstBinExpr(IOperation.COMPNE, left, right);
			break;
		case EulangParser.GREATER:
			binop = new AstBinExpr(IOperation.COMPGT, left, right);
			break;
		case EulangParser.COMPGE:
			binop = new AstBinExpr(IOperation.COMPGE, left, right);
			break;
		case EulangParser.LESS:
			binop = new AstBinExpr(IOperation.COMPLT, left, right);
			break;
		case EulangParser.COMPLE:
			binop = new AstBinExpr(IOperation.COMPLE, left, right);
			break;

		case EulangParser.AND:
			binop = new AstBinExpr(IOperation.COMPAND, left, right);
			break;
		case EulangParser.OR:
			binop = new AstBinExpr(IOperation.COMPOR, left, right);
			break;

		case EulangParser.BITOR:
			binop = new AstBinExpr(IOperation.BITOR, left, right);
			break;
		case EulangParser.BITXOR:
			binop = new AstBinExpr(IOperation.BITXOR, left, right);
			break;
		case EulangParser.BITAND:
			binop = new AstBinExpr(IOperation.BITAND, left, right);
			break;

		default:
			unhandled(tree);
			return null;
		}
		getSource(tree, binop);
		return binop;
	}

	public IAstUnaryExpr constructUnaryExpr(Tree tree) throws GenerateException {
		assert (tree.getChildCount() == 1);
		IAstTypedExpr expr = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstUnaryExpr unary = null;

		switch (tree.getType()) {
		case EulangParser.INV:
			unary = new AstUnaryExpr(IOperation.INV, expr);
			break;
		// case EulangParser.NOT:
		// unary = new AstUnaryExpr(IOperation.NOT, expr);
		// break;
		case EulangParser.NEG:
			unary = new AstUnaryExpr(IOperation.NEG, expr);
			break;
		case EulangParser.POSTINC:
			unary = new AstUnaryExpr(IOperation.POSTINC, expr);
			break;
		case EulangParser.POSTDEC:
			unary = new AstUnaryExpr(IOperation.POSTDEC, expr);
			break;
		case EulangParser.PREINC:
			unary = new AstUnaryExpr(IOperation.PREINC, expr);
			break;
		case EulangParser.PREDEC:
			unary = new AstUnaryExpr(IOperation.PREDEC, expr);
			break;

		default:
			unhandled(tree);
			return null;
		}
		getSource(tree, unary);
		return unary;
	}

	public IAstBinExpr constructLogicalNot(Tree tree) throws GenerateException {
		assert (tree.getChildCount() == 1);
		IAstTypedExpr expr = checkConstruct(tree.getChild(0),
				IAstTypedExpr.class);
		IAstLitExpr zero = createZero(expr.getType());
		getEmptySource(tree, zero);
		IAstBinExpr binary = new AstBinExpr(IOperation.COMPNE, expr, zero);

		getSource(tree, binary);
		return binary;
	}

	/**
	 * @param type
	 * @return
	 */
	private IAstLitExpr createZero(LLType type) {
		if (type != null) {
			switch (type.getBasicType()) {
			case BOOL:
				return new AstBoolLitExpr("false", type, false);
			case INTEGRAL:
				return new AstIntLitExpr("0", type, 0);
			case FLOATING:
				return new AstFloatLitExpr("0", type, 0.0);
				//$FALL-THROUGH$
			}
		}
		return new AstIntLitExpr("0", typeEngine.getIntType(type != null ? type
				.getBits() : 1), 0);
	}

	/**
	 * @param tree
	 * @return
	 */
	public IAstReturnStmt constructReturn(Tree tree) throws GenerateException {
		IAstTypedExpr expr = null;
		if (tree.getChildCount() == 1) {
			expr = checkConstruct(tree.getChild(0), IAstTypedExpr.class);
		}
		IAstReturnStmt stmt = new AstReturnStmt(expr);
		getSource(tree, stmt);
		return stmt;
	}

	/**
	 * @param tree
	 * @return
	 */
	public IAstNode constructArgDef(Tree tree) throws GenerateException {
		int argIdx = 0;

		boolean isVar = false;
		boolean isMacro = false;
		if (tree.getChild(argIdx).getType() == EulangParser.MACRO) {
			isMacro = true;
			argIdx++;
		}

		if (tree.getChild(argIdx).getType() == EulangParser.AT) {
			isVar = true;
			argIdx++;
		}

		IAstSymbolExpr symExpr = createSymbol(tree.getChild(argIdx++));

		IAstType type = null;
		IAstTypedExpr defaultVal = null;

		if (tree.getChildCount() > argIdx) {
			if (tree.getChild(argIdx).getType() == EulangParser.TYPE) {
				type = checkConstruct(tree.getChild(argIdx), IAstType.class);
				argIdx++;
			}
			if (argIdx < tree.getChildCount()) {
				defaultVal = checkConstruct(tree.getChild(argIdx),
						IAstTypedExpr.class);
			}
		}

		if (isMacro && isVar)
			throw new GenerateException(tree,
					"cannot have a macro and '@' argument");

		IAstArgDef argDef = new AstArgDef(symExpr, type, defaultVal, isMacro,
				isVar);
		getSource(tree, argDef);

		symExpr.getSymbol().setDefinition(argDef);

		/*
		 * if (type != null && type.getType() != null) {
		 * symExpr.getSymbol().setType(type.getType()); }
		 */

		return argDef;
	}

	/**
	 * @param tree
	 * @return
	 */
	public IAstNodeList<IAstStmt> constructStmtList(Tree tree) {
		IAstNodeList<IAstStmt> list = new AstNodeList<IAstStmt>(IAstStmt.class);

		assert tree.getType() == EulangParser.STMTLIST;

		for (Tree kid : iter(tree)) {
			try {
				IAstStmt node = checkConstruct(kid, IAstStmt.class);
				if (node instanceof TempLabelStmt) {
					list.add(((TempLabelStmt) node).getLabel());
					list.add(((TempLabelStmt) node).getStmt());
				} else if (node != null) {
					list.add(node);
					node.setParent(list);
				}
			} catch (GenerateException e) {
				emitExceptionError(e);
			}
		}

		getSource(tree, list);
		return list;
	}

	private void emitExceptionError(GenerateException e) {
		if (e.getTree() != null)
			error(e.getTree(), e.getMessage());
		else
			error(e.getSourceRef(), e.getMessage());
	}

	/**
	 * @param tree
	 * @return
	 */
	public IAstPrototype constructPrototype(Tree tree) throws GenerateException {
		IAstType retTypeNode;
		int start = 1;
		if (tree.getChildCount() == 0
				|| tree.getChild(0).getType() == EulangParser.ARGDEF) {
			retTypeNode = new AstType(typeEngine.UNSPECIFIED);
			getEmptySource(tree, retTypeNode);
			start = 0;
		} else if (tree.getChild(0).getType() == EulangParser.TUPLE) {
			retTypeNode = constructType(tree.getChild(0));
			getSource(tree, retTypeNode);
		} else {
			retTypeNode = constructType(tree.getChild(0).getChild(0));
			getSource(tree, retTypeNode);
		}

		IAstArgDef[] argTypes = new IAstArgDef[tree.getChildCount() - start];
		int idx = 0;
		while (start < tree.getChildCount()) {
			argTypes[idx++] = checkConstruct(tree.getChild(start++),
					IAstArgDef.class);
		}

		IAstPrototype proto = new AstPrototype(typeEngine, retTypeNode,
				argTypes);
		getSource(tree, proto);
		return proto;
	}

	/**
	 * @param child
	 * @return
	 */
	public IAstType constructType(Tree tree) throws GenerateException {
		IAstType type = null;
		if (tree.getType() == EulangParser.NIL) {
			type = new AstType(typeEngine.VOID);
		} else {
			Tree kid0 = tree.getChild(0);
			if (tree.getType() == EulangParser.TUPLE) {
				LLType[] tupleTypes = new LLType[tree.getChildCount()];
				for (int idx = 0; idx < tree.getChildCount(); idx++) {
					assert tree.getChild(idx).getType() == EulangParser.TYPE;
					tupleTypes[idx] = constructType(tree.getChild(idx)
							.getChild(0)).getType();	// TODO
				}
				type = new AstType(new LLTupleType(typeEngine, tupleTypes));
			} else if (tree.getType() == EulangParser.ARRAY) {
				IAstTypedExpr countExpr = null;
				if (tree.getChild(1) != null) {
					countExpr = checkConstruct(tree.getChild(1),
							IAstTypedExpr.class);
				}
				assert kid0.getType() == EulangParser.TYPE;
				type = new AstArrayType(constructType(kid0.getChild(0)), countExpr);
			} else if (tree.getType() == EulangParser.REF) {
				LLType baseType = constructType(kid0).getType();// TODO
				type = new AstType(typeEngine.getRefType(baseType)); 
			} else if (tree.getType() == EulangParser.CODE) {
				if (tree.getChildCount() == 0) {
					type = new AstType(typeEngine.getCodeType(null, (LLType[]) null));
				}
				else { 
					if (tree.getChildCount() > 1)
						throw new GenerateException(tree.getChild(2),
								"did not expect code block here");
					IAstPrototype proto = checkConstruct(kid0, IAstPrototype.class);
					if (proto.hasDefaultArguments()) {
						throw new GenerateException(tree.getChild(2),
								"cannot use default arguments in code type");
					}
					type = new AstType(typeEngine.getCodeType(proto.returnType(), proto
							.argumentTypes()));
				}
			} else if (tree.getType() == EulangParser.IDREF) {
				IAstSymbolExpr symbolExpr = checkConstruct(tree,
						IAstSymbolExpr.class);

				if (symbolExpr.getType() != null)
					type = new AstType(symbolExpr.getType());
				else
					type = new AstNamedType(null, symbolExpr);
			}
		}
		if (type == null)
			unhandled(tree);
		
		getSource(tree, type);
		return type;
	}

	public IAstType constructTypeExpr(Tree tree) throws GenerateException {
		IAstType typeExpr = null;
		if (tree.getChildCount() == 1)
			typeExpr = constructType(tree.getChild(0));
		else {
			typeExpr = new AstType(null);
			getSource(tree, typeExpr);
			
		}
		return typeExpr;
	}

	@SuppressWarnings("unchecked")
	public IAstDefineStmt constructDefine(Tree tree) throws GenerateException {
		IAstSymbolExpr symbolExpr = createSymbol(tree.getChild(0));

		IAstDefineStmt stmt;
		Tree exprTree = tree.getChild(1);
		if (exprTree.getType() == EulangParser.LIST) {
			IAstNodeList<IAstTypedExpr> exprList = checkConstruct(exprTree,
					IAstNodeList.class);
			stmt = new AstDefineStmt(symbolExpr, exprList.list());
		} else {
			IAstTypedExpr expr = null;
			expr = checkConstruct(exprTree, IAstTypedExpr.class);
			stmt = new AstDefineStmt(symbolExpr, Collections
					.singletonList(expr));
		}
		getSource(tree, stmt);

		symbolExpr.getSymbol().setDefinition(stmt);

		return stmt;
	}

	/**
	 * @param tree
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public IAstTypedExpr constructCodeExpr(Tree tree) throws GenerateException {
		assert tree.getChildCount() > 0;
		boolean isMacro = tree.getType() == EulangParser.MACRO;
		pushScope(new LocalScope(currentScope));
		try {
			IAstPrototype proto;
			int idx = 0;
			if (tree.getChild(idx).getType() == EulangParser.PROTO) {
				proto = checkConstruct(tree.getChild(idx), IAstPrototype.class);

				boolean hitDefaults = false;
				for (IAstArgDef argDef : proto.argumentTypes()) {
					if (!isMacro && argDef.isMacro()) {
						throw new GenerateException(argDef.getSourceRef(),
								"cannot use macro arguments outside macro code");
					}
					if (argDef.getDefaultValue() != null) {
						if (!isMacro)
							throw new GenerateException(argDef.getSourceRef(),
									"cannot use default arguments outside macro code");
						hitDefaults = true;
					} else if (hitDefaults) {
						throw new GenerateException(argDef.getSourceRef(),
								"non-default argument follows default argument");
					}
				}
				idx++;
			} else {
				IAstType unspecified = new AstType(null);
				getEmptySource(tree, unspecified);
				proto = new AstPrototype(typeEngine, unspecified,
						new IAstArgDef[0]);
				getEmptySource(tree, proto);
			}
			IAstNodeList<IAstStmt> list = null;
			if (idx < tree.getChildCount())
				list = checkConstruct(tree.getChild(idx++), IAstNodeList.class);
			IAstCodeExpr codeExpr = new AstCodeExpr(proto, currentScope, list,
					isMacro);
			getSource(tree, codeExpr);
			return codeExpr;
		} finally {
			popScope(tree);
		}
	}

	/**
	 * @param tree
	 * @return
	 */
	private IAstLitExpr constructLiteral(Tree tree) throws GenerateException {
		assert tree.getType() == EulangParser.LIT;
		assert tree.getChildCount() == 1;

		IAstLitExpr litExpr = null;

		String lit = tree.getChild(0).getText();
		switch (tree.getChild(0).getType()) {
		case EulangParser.NIL:
			litExpr = new AstNilLitExpr(lit, typeEngine.NIL);
			break;
		case EulangParser.TRUE:
			litExpr = new AstBoolLitExpr(lit, typeEngine.BOOL, true);
			break;
		case EulangParser.FALSE:
			litExpr = new AstBoolLitExpr(lit, typeEngine.BOOL, false);
			break;
		case EulangParser.NUMBER:
			try {
				Long l = Long.parseLong(lit);
				litExpr = new AstIntLitExpr(lit, typeEngine.INT, l);
			} catch (NumberFormatException e) {
				try {
					Double d = Double.parseDouble(lit);
					litExpr = new AstFloatLitExpr(lit, typeEngine.FLOAT, d);
				} catch (NumberFormatException e2) {
				}
			}
		}

		if (litExpr == null) {
			unhandled(tree);
			return null;
		}
		getSource(tree, litExpr);
		return litExpr;

	}

	/**
	 * @return
	 */
	public TypeEngine getTypeEngine() {
		return typeEngine;
	}

}
