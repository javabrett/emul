/**
 * 
 */
package org.ejs.eulang.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.ejs.eulang.ITarget;
import org.ejs.eulang.Message;
import org.ejs.eulang.TargetV9t9;
import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.DumpAST;
import org.ejs.eulang.ast.ExpandAST;
import org.ejs.eulang.ast.GenerateAST;
import org.ejs.eulang.ast.IAstAddrOfExpr;
import org.ejs.eulang.ast.IAstAllocStmt;
import org.ejs.eulang.ast.IAstCodeExpr;
import org.ejs.eulang.ast.IAstDefineStmt;
import org.ejs.eulang.ast.IAstDerefExpr;
import org.ejs.eulang.ast.IAstModule;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstScope;
import org.ejs.eulang.ast.IAstTypedExpr;
import org.ejs.eulang.ast.IAstTypedNode;
import org.ejs.eulang.ast.impl.AstTypedNode;
import org.ejs.eulang.ext.CommandLauncher;
import org.ejs.eulang.llvm.LLBlock;
import org.ejs.eulang.llvm.LLCodeVisitor;
import org.ejs.eulang.llvm.LLModule;
import org.ejs.eulang.llvm.LLVMEnv;
import org.ejs.eulang.llvm.LLVMGenerator;
import org.ejs.eulang.llvm.instrs.LLInstr;
import org.ejs.eulang.llvm.instrs.LLTypedInstr;
import org.ejs.eulang.llvm.ops.LLOperand;
import org.ejs.eulang.llvm.parser.LLParserHelper;
import org.ejs.eulang.llvm.parser.LLVMLexer;
import org.ejs.eulang.llvm.parser.LLVMParser;
import org.ejs.eulang.parser.EulangLexer;
import org.ejs.eulang.parser.EulangParser;
import org.ejs.eulang.symbols.GlobalScope;
import org.ejs.eulang.symbols.IScope;
import org.ejs.eulang.symbols.ISymbol;
import org.ejs.eulang.types.LLCodeType;
import org.ejs.eulang.types.LLPointerType;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeInference;
import org.junit.Before;

/**
 * @author ejs
 *
 */
public class BaseTest {

	private static final String DIR = File.separatorChar == '\\' ? "c:/temp/" : "/tmp/";

	/**
	 * 
	 */
	public BaseTest() {
		super();
	}
	
	protected ParserRuleReturnScope parse(String method, String str, boolean expectError)
			throws RecognitionException {
		System.err.flush();
		System.out.flush();
		final StringBuilder errors = new StringBuilder();
		try {
	    	// create a CharStream that reads from standard input
	        EulangLexer lexer = new EulangLexer(new ANTLRStringStream(str)) {
	        	/* (non-Javadoc)
	        	 * @see org.antlr.runtime.BaseRecognizer#emitErrorMessage(java.lang.String)
	        	 */
	        	@Override
	        	public void emitErrorMessage(String msg) {
	        		errors.append( msg +"\n");
	        	}
	        };
	        
	        // create a buffer of tokens pulled from the lexer
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        // create a parser that feeds off the tokens buffer
	        EulangParser parser = new EulangParser(tokens);
	        // begin parsing at rule
	        ParserRuleReturnScope prog = null;
	        if(method == null)
	        	prog = parser.prog();
	        else {
	    		try {
					prog = (ParserRuleReturnScope) parser.getClass().getMethod(method).invoke(parser);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
	        		
	        }
	        if (dumpTreeize)
	        	System.out.println("\n"+lineize(str));
	        
	        if (!expectError) {
				if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
					System.err.println(errors);
					fail(errors.toString());
				}
			} else {
				assertTrue(parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0);
			}
	        
	        if (dumpTreeize && prog != null && prog.getTree() != null)
	        	System.out.println(((Tree) prog.getTree()).toStringTree());
	
	        if (!expectError)
	        	assertTrue("did not consume all input", tokens.index() >= tokens.size());
	
	        return prog;
		} finally {
			System.err.flush();
			System.out.flush();
			
		}
	}

	protected void parse(String str) throws Exception {
		parse(null, str, false);
	}

	protected void parseFail(String str) throws Exception {
		parse(null, str, true);
	}

	protected void parseAt(String method, String str) throws Exception {
		parse(method, str, false);
	}

	protected ITarget v9t9Target = new TargetV9t9();
	protected TypeEngine typeEngine = v9t9Target.getTypeEngine();;
	protected boolean dumpSimplify;
	protected boolean dumpTreeize;
	protected boolean dumpTypeInfer;
	protected boolean dumpExpand;
	protected boolean dumpFrontend;

	@Before
	public void initTypeEngine() {
		v9t9Target = new TargetV9t9();
		typeEngine = v9t9Target.getTypeEngine();
	}
	protected IAstNode treeize(String method, String str, boolean expectError) throws Exception {
    	ParserRuleReturnScope ret = null;
    	try {
    		ret = parse(method, str, false);
    	} catch (AssertionFailedError e) {
    		if (expectError)
    			return null;
    		throw e;
    	}
    	if (ret == null)
    		return null;
    	
    	Tree tree = (Tree) ret.getTree();
    	if (tree == null)
    		return null;
    	
    	
    	Map<CharStream, String> fileMap = new HashMap<CharStream, String>();
		String mainFileName = "<string>";
		GenerateAST gen = new GenerateAST(typeEngine, mainFileName, fileMap);
    	
    	IAstNode node = null;
    		
    	if(method == null) {
        	node = gen.constructModule(tree);
        	((IAstModule) node).getNonFileText().put(mainFileName, str);
    	} else {
    		try {
				node = (IAstNode) gen.getClass().getMethod(method).invoke(tree);
			} catch (Exception e) {
				throw e;
			}
    	}
	 
    	if (dumpTreeize || (!expectError && gen.getErrors().size() > 0)) {
	    	DumpAST dump = new DumpAST(System.out);
	    	node.accept(dump);
    	}
     	
    	if (!expectError) {
    		 if (gen.getErrors().size() > 0) {
    			 String msgs = catenate(gen.getErrors());
    			 fail(msgs);
    		 }
    	} else {
    		 if (gen.getErrors().isEmpty()) {
    			 fail("no errors generated");
    		 }
    	}
    		 
    	return node;
    }

	protected IAstNode treeize(String str, boolean expectError) throws Exception {
		return treeize(null, str, expectError);
	}

    /**
	 * @param errors
	 * @return
	 */
	protected String catenate(Collection<? extends Message> errors) {
		StringBuilder sb = new StringBuilder();
		for (Message e : errors) {
			sb.append(e.toString());
			sb.append('\n');
		}
		return sb.toString();
	}

	protected IAstModule treeize(String str) throws Exception {
    	return (IAstModule) treeize(null, str, false);
    }
	protected IAstModule treeizeFail(String str) throws Exception {
    	return (IAstModule) treeize(null, str, true);
    }
    
    
    /**
     * @param symIds 
     * @param nodeIds 
     * @param mod
     */
    protected void doSanityTest(IAstNode node, Set<Integer> nodeIds) {
    	assertFalse(nodeIds.contains(node.getId()));
    	nodeIds.add(node.getId());
    	if (node instanceof IAstScope)
    		scopeTest(((IAstScope) node).getScope());
    	assertNotNull(node+"", node);
    	assertNotNull(node+"", node.getChildren());
    	assertNotNull("source for " + node.getClass()+"", node.getSourceRef());
    	for (IAstNode kid : node.getChildren()) {
    		assertSame(kid+"",  node, kid.getParent());
    		assertEquals(node, kid.getParent());
    		
    		if (node instanceof IAstScope && kid instanceof IAstScope) {
    			assertEquals(((IAstScope)node).getScope(), ((IAstScope) kid).getScope().getParent());
    		}
    		doSanityTest(kid, nodeIds);
    		
    		//assertTrue("source containment " + kid+"", node.getSourceRef().contains(kid.getSourceRef()));
    	}
    	
    }
    protected void sanityTest(IAstNode node) {
    	Set<Integer> nodeIds = new HashSet<Integer>();
    	doSanityTest(node, nodeIds);
    	
    	IAstNode copy = node.copy();
    	
    	//DumpAST dump = new DumpAST(System.out);
    	//copy.accept(dump);
    	checkCopy(node, copy);
    }

	/**
	 * Make sure a full copy works
	 * @param node
	 * @param copy
	 */
	private void checkCopy(IAstNode node, IAstNode copy) {
		assertEquals(node.getClass(), copy.getClass());
		assertFalse(node.toString(), node == copy);
		if (node.getParent() != null)
			assertFalse(node.toString(), node.getParent() == copy.getParent());
		IAstNode[] kids = node.getChildren();
		IAstNode[] copyKids = copy.getChildren();
		assertEquals(node.toString() +  ": children count differ", kids.length, copyKids.length);
		if (node instanceof IAstTypedNode)
			assertEquals(node.toString() + ": types differ", ((IAstTypedNode) node).getType(), ((IAstTypedNode) copy).getType());
		
		if (node instanceof IAstScope) {
			IScope scope = ((IAstScope) node).getScope();
			IScope copyScope = ((IAstScope) copy).getScope();
			assertFalse(node.toString(), scope == copyScope);
			assertEquals(node.toString() + ": scope count", scope.getSymbols().length, copyScope.getSymbols().length);
			
			for (ISymbol symbol : scope) {
				ISymbol copySym = copyScope.get(symbol.getUniqueName());
				assertSame(copyScope, copySym.getScope());
				assertEquals(symbol+"", symbol, copySym);
				// a symbol may refer to dead code
				if (symbol.getDefinition() == null || symbol.getDefinition().getParent() != null)
					assertEquals(symbol+"", symbol.getDefinition(), copySym.getDefinition());
				assertFalse(symbol+"", symbol == copySym);
				assertFalse(symbol+"", symbol.getDefinition() != null && symbol.getDefinition() == copySym.getDefinition());
				if (symbol.getDefinition() != null) {
					assertFalse(symbol+"", symbol.getDefinition() == copySym.getDefinition());
				}
			}
			
			assertEquals(node.toString() + ": scopes differ", scope, copyScope);
		}
		
		for (int  i = 0; i < kids.length; i++) {
			assertSame(copy+"", copy, copyKids[i].getParent());
			// look out for dead definitions
			if (kids[i].getParent() == copy)
				checkCopy(kids[i], copyKids[i]);
		}
		
		// now that a rough scan worked, check harder
		assertEquals(node.toString() + ": #equals()", node, copy);
		
		assertEquals(node.toString() + ": #hashCode()", node.hashCode(), copy.hashCode());
	}

	/**
	 * @param scope
	 */
	protected void scopeTest(IScope scope) {
		if (!(scope instanceof GlobalScope))
			assertNotNull(scope.getOwner());
		Set<Integer> seenIds = new HashSet<Integer>();
		Set<String> seen = new HashSet<String>();
		for (ISymbol sym : scope) {
			assertNotNull(sym);
			assertNotNull(sym.getName());
			assertNotNull(sym.getUniqueName());
			assertFalse(sym.getUniqueName(), seen.contains(sym.getUniqueName()));
			seen.add(sym.getUniqueName());
			assertSame(scope, sym.getScope());
			assertFalse(sym.getUniqueName(), seenIds.contains(sym.getNumber()));
			seenIds.add(sym.getNumber());
		}
	}
	
	protected void typeTest(IAstNode node, boolean allowUnknown) {
		if (!allowUnknown && node instanceof IAstTypedNode) {
			if (!(node instanceof IAstDefineStmt) && !(node.getParent() instanceof IAstDefineStmt)) {
				assertNotNull("No type: " +node.toString(), ((IAstTypedNode) node).getType());
				assertTrue("Unresolved type: " +node.toString(), ((IAstTypedNode) node).getType().isComplete());
			}
		}
		for (IAstNode kid : node.getChildren()) {
			assertNotNull(node.toString(), kid);
			typeTest(kid, allowUnknown);
		}
	}
 
	protected void doTypeInfer(IAstNode mod) {
		doTypeInfer(mod, false);
	}
	protected void doTypeInfer(IAstNode mod, boolean expectErrors) {
		TypeInference infer = new TypeInference(typeEngine);
		Collection<Message> messages = infer.getMessages();
		
		if (dumpTypeInfer) {
			System.out.println("Before inference:");
			DumpAST dump = new DumpAST(System.out);
			mod.accept(dump);
		}
		infer.infer(mod, true);
		if (dumpTypeInfer || (!expectErrors && messages.size() > 0)) {
			DumpAST dump = new DumpAST(System.out);
			mod.accept(dump);
		}
		//System.out.println("Inference: " + passes + " passes");
		for (Message msg : messages)
			System.err.println(msg);
		if (!expectErrors)
			assertEquals("expected no errors: " + catenate(messages), 0, messages.size());
		else
			assertTrue("expected errors", messages.size() > 0);
	}
	
	protected void doSimplify(IAstNode mod) {
		
		// must infer types first
		doTypeInfer(mod);
		
		int depth = mod.getDepth();
		
		int passes = 0;
		while (passes++ <= depth) {
			boolean changed = mod.simplify(typeEngine);
			
			if (!changed) 
				break;
			
			if (dumpSimplify) {
				System.err.flush();
				System.out.println("After simplification:");
				DumpAST dump = new DumpAST(System.out);
				mod.accept(dump);
			}
			
		}
		if (dumpSimplify)
			System.out.println("Simplification: " + passes + " passes");
	}
	
	

	protected IAstNode doExpand(IAstNode node, boolean instances, boolean expectErrors) {
		
		// expansion requires initial inference
    	TypeInference infer = new TypeInference(typeEngine);
    	infer.infer(node, false);
    	sanityTest(node);
    	
    	
		ExpandAST expand = new ExpandAST(typeEngine, instances);
		
		List<Message> messages = new ArrayList<Message>();
		boolean hadAnyErrors = false;
		for (int passes = 1; passes < 256; passes++) {
			messages.clear();
			node = expand.expand(messages, node);
			
			if (expand.isChanged()) {
				if (dumpExpand || messages.size() > 0) {
					System.out.println("After expansion pass " + passes + ":");
					DumpAST dump = new DumpAST(System.out);
					node.accept(dump);
				}
				
				for (Message msg : messages) {
					System.err.println(msg);
					hadAnyErrors = true;
				}
				if (!expectErrors)
					assertEquals(catenate(messages), 0, messages.size());
			} else {
				break;
			}
		}

		messages.clear();
		expand.validate(messages, node);
		for (Message msg : messages) {
			System.err.println(msg);
			hadAnyErrors = true;
		}
		if (!expectErrors)
			assertEquals(catenate(messages), 0, messages.size());
		else if (!hadAnyErrors || messages.isEmpty())
			fail("no messages generated");
		return node;
	}
	protected IAstNode doExpand(IAstNode node, boolean expectErrors) {
		return doExpand(node, false, expectErrors);
	}

	protected IAstNode doExpand(IAstNode node) {
		return doExpand(node, false);
	}
	protected boolean isCastTo(IAstTypedExpr expr, LLType type) {
		return AstTypedNode.getCastedChild(expr) != null && expr.getType().equals(type);
	}
	
	protected IAstModule doFrontend(IAstModule mod, boolean expectErrors) throws Exception {
		DumpAST dump = new DumpAST(System.out);
		boolean failed= false;
		IAstModule expanded = mod;
		try {
			expanded = (IAstModule) doExpand(mod);
	    	sanityTest(expanded);
	    	
	    	if (dumpFrontend) {
		    	System.err.flush();
				System.out.println("After doExpand:");
				expanded.accept(dump);
	    	}
	    	
	    	doTypeInfer(expanded);
	    	doSimplify(expanded);
	    	
	    	if (expectErrors)
	    		failed = true;
		} catch (AssertionError e) {
			if (!expectErrors)
				throw e;
		}
		if (failed)
    		fail("expected errors");

		if (dumpFrontend) {
	    	System.err.flush();
			System.out.println("After frontend:");
			dump = new DumpAST(System.out);
			expanded.accept(dump);
		}
		
    	return expanded;
	}

	protected IAstModule doFrontend(String text) throws Exception {
		if (dumpLLVMGen)
			System.out.println(text);
		IAstModule mod = treeize(text);
    	sanityTest(mod);
    	
    	return doFrontend(mod, false);
	}

	protected boolean dumpLLVMGen;
	protected boolean doAssemble = true;
	protected boolean doOptimize;
	protected boolean doLLVMOptimize;
	
	/**
	 * Generate the module, expecting no errors.
	 * @param mod
	 * @return 
	 */
	protected LLVMGenerator doGenerate(IAstModule mod) throws Exception {
		return doGenerate(mod, false);
	}
	/**
	 * @param mod
	 * @return 
	 * @throws Exception 
	 */
	protected LLVMGenerator doGenerate(IAstModule mod, boolean expectErrors) throws Exception {
		if (dumpLLVMGen) {
			System.out.println("Before generating:");
			DumpAST dump = new DumpAST(System.out);
			mod.accept(dump);
		}
		LLVMGenerator generator = new LLVMGenerator(v9t9Target);
		generator.generate(mod);
		
		List<Message> messages = generator.getMessages();
		for (Message msg : messages)
			System.err.println(msg);
		if (!expectErrors)
			assertEquals("expected no errors: " + catenate(messages), 0, messages.size());
		else if (!messages.isEmpty())
			return generator;
		
		if (doAssemble) {
			File file = getTempFile("");
			doAssemble(generator, file, expectErrors);
		}
		
		if (expectErrors)
			assertTrue("expected errors", messages.size() > 0);
		
		
		return generator;
	}

	protected void doAssemble(LLVMGenerator generator, File file, boolean expectErrors) throws Exception
	{
		String text = generator.getUnoptimizedText();
		doAssemble(text, file, expectErrors);
		if (doLLVMOptimize) {
			String optText = doOptimize(file, new File(file.getAbsolutePath() + ".ll"), new File(file.getAbsolutePath() + ".bc"));
			generator.setOptimizedText(optText);
		}
	}
	
	protected void doAssemble(String text, File file, boolean expectErrors)
			throws IOException, FileNotFoundException, CoreException,
			AssertionFailedError {
		File llfile = new File(file.getAbsolutePath() + ".ll");
		FileOutputStream os = new FileOutputStream(llfile);
		os.write(text.getBytes());
		os.close();
		
		File bcFile = new File(file.getAbsolutePath() + ".bc");
		bcFile.delete();

		if (dumpLLVMGen)
			System.out.println(lineize(text));

		try {
			run(LLVMEnv.getAs(), llfile.getAbsolutePath(), "-f", "-o", bcFile.getAbsolutePath());
		} catch (AssertionFailedError e) {
			if (expectErrors)
				return;
			else
				throw e;
		}
	}

	protected String doOptimize(String lltext, File file) throws Exception {
		doAssemble(lltext, file, false);
		
		String optText = doOptimize(file, new File(file.getAbsolutePath() + ".ll"), new File(file.getAbsolutePath() + ".bc"));
		return optText;

	}
	
	protected String doOptimize(File file, File llfile,
			File bcFile) throws CoreException,
			IOException, AssertionFailedError {
		File bcOptFile = new File(file.getAbsolutePath() + ".opt.bc");
		bcOptFile.delete();

		File llOptFile = new File(file.getAbsolutePath() + ".opt.ll");
		llOptFile.delete();

		String opts = LLVMEnv.getStdOpts();
		
		run(LLVMEnv.getAs(), llfile.getAbsolutePath(), "-f", "-o", bcFile.getAbsolutePath());
		List<String> optList = new ArrayList<String>();
		if (opts.length() > 0)
			optList.addAll(Arrays.asList(opts.split(" ")));
		for (Iterator<String> iter = optList.iterator(); iter.hasNext(); ) {
			String val = iter.next();
			if (val.startsWith("//")) {
				iter.remove();
			}
		}
		optList.add(0, bcFile.getAbsolutePath());
		optList.add("-f");
		optList.add("-o");
		optList.add(bcOptFile.getAbsolutePath());
		run(LLVMEnv.getOpt(), (String[]) optList.toArray(new String[optList.size()]));
		runAndReturn(LLVMEnv.getDis(), bcOptFile.getAbsolutePath(), "-f", "-o", llOptFile.getAbsolutePath());
		return readFile(llOptFile.getAbsoluteFile());
	}
	/**
	 * @param absoluteFile
	 * @return
	 * @throws IOException 
	 */
	private String readFile(File absoluteFile) throws IOException {
		long len = absoluteFile.length();
		FileInputStream fis = new FileInputStream(absoluteFile);
		try {
			byte[] bytes = new byte[(int)len];
			fis.read(bytes);
			return new String(bytes);
		} finally {
			fis.close();
		}
	}

	/**
	 * @param string
	 * @param absolutePath
	 * @param string2
	 * @param string3
	 * @param absolutePath2
	 * @throws CoreException 
	 */
	private String runAndReturn(String prog, String... args) throws CoreException {
		CommandLauncher launcher = new CommandLauncher();
		launcher.showCommand(true);
		launcher.execute(new Path(prog), 
				args,
				null,
				null,
				null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		int exit = launcher.waitAndRead(out, err);
		
		if (dumpLLVMGen)
			System.out.print(out.toString());
		System.err.print(err.toString());
		assertEquals(out.toString() + err.toString(), 0, exit);
		return out.toString();
	}

	private void run(String prog, String... args) throws CoreException {
		runAndReturn(prog, args);
	}
	/**
	 * @return
	 * @throws IOException 
	 */
	protected File getTempFile(String ext) throws IOException {
		String name = "test";
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for (StackTraceElement e : stackTrace) {
			if (e.getMethodName().startsWith("test")) {
				name = e.getMethodName();
				break;
			}
		}
		return new File(DIR + name + ext);
	}

	protected IAstTypedExpr getMainExpr(IAstDefineStmt def) {
		return def.getMatchingBodyExpr(null);
	}

	protected void assertFoundInUnoptimizedText(String string, LLVMGenerator generator) {
		String unopt = generator.getUnoptimizedText();
		assertTrue(string + " in\n"+ unopt, unopt.replaceAll("\\s+","").contains(string.replaceAll("\\s+","")));
		
	}
	protected void assertFoundInOptimizedText(String string, LLVMGenerator generator) {
		String opt = generator.getOptimizedText();
		assertTrue(string + " in\n"+ opt, opt.replaceAll("\\s+","").contains(string.replaceAll("\\s+","")));
		
	}
	protected void assertMatchText(String regex, String theText) {
		Matcher matcher = Pattern.compile(regex).matcher(theText);
		if (!matcher.find())
			fail(regex + " in\n"+ theText);
		
	}
	
	protected IAstTypedExpr getValue(IAstTypedExpr expr) {
		if (expr instanceof IAstDerefExpr)
			return ((IAstDerefExpr)expr).getExpr();
		return expr;
	}

	protected IAstTypedExpr getMainBodyExpr(IAstDefineStmt def) {
		return def.getMatchingBodyExpr(null);
	}

	protected LLModule getModule(String text) throws Exception {
		IAstModule mod = doFrontend(text);
		LLVMGenerator gen = doGenerate(mod);
		
		if (doLLVMOptimize) {
			return doLLVMParse(gen.getOptimizedText());
		} else {
			return gen.getModule();
		}
	}
	
	protected IAstCodeExpr getCodePtrValue(IAstAllocStmt astmt) {
    	assertTrue(astmt.getType() instanceof LLPointerType);
    	assertTrue(astmt.getType().getSubType() instanceof LLCodeType);
    	IAstAddrOfExpr addrOf = (IAstAddrOfExpr) astmt.getExprs().getFirst();
    	IAstCodeExpr code = (IAstCodeExpr) addrOf.getExpr();
		return code;
	}

	protected LLModule doLLVMParse(String text) throws Exception {
		return doLLVMParse(text, false);
	}

	protected LLModule doLLVMParse(String text, boolean expectError) throws Exception {
		GlobalScope globalScope = new GlobalScope();
		LLModule mod = new LLModule(typeEngine, v9t9Target, globalScope);
		LLParserHelper helper = new LLParserHelper(mod);
		
		doLLVMParse(helper, text, expectError);
		
		// finalize types
		mod.finalizeTypes();
		
		for (ISymbol sym : mod.getTypeScope()) {
			if (!(sym.getType() != null && sym.getType().isComplete()))
				fail(sym+": type");	
		}
		
		assertTrue(helper.getForwardTypes().isEmpty());
		StringBuilder ssb = new StringBuilder();
		for (String name : helper.getForwardSymbols().keySet()) {
			ssb.append(name).append(' ');
		}
		if (ssb.length() > 0)
			fail("Undefined symbols: " + ssb.toString());
		
		mod.accept(new LLCodeVisitor() {
			/* (non-Javadoc)
			 * @see org.ejs.eulang.llvm.LLCodeVisitor#enterInstr(org.ejs.eulang.llvm.LLBlock, org.ejs.eulang.llvm.instrs.LLInstr)
			 */
			@Override
			public boolean enterInstr(LLBlock block, LLInstr instr) {
				if (instr instanceof LLTypedInstr) {
					LLType typ = ((LLTypedInstr) instr).getType();
					assertTrue(instr+":"+typ, typ != null && typ.isComplete());
				}
				return true;
			}
			/* (non-Javadoc)
			 * @see org.ejs.eulang.llvm.LLCodeVisitor#enterOperand(org.ejs.eulang.llvm.instrs.LLInstr, int, org.ejs.eulang.llvm.ops.LLOperand)
			 */
			@Override
			public boolean enterOperand(LLInstr instr, int num,
					LLOperand operand) {
				assertTrue(instr+":"+num+":"+operand, operand != null && operand.getType()!= null && operand.getType().isComplete());
				return false;
			}
		});
		
		return mod;
	}
	
	/**
	 * @param text
	 * @return
	 */
	protected ParserRuleReturnScope doLLVMParse(LLParserHelper helper, String str, boolean expectError) throws RecognitionException {
		System.err.flush();
		System.out.flush();
		final StringBuilder errors = new StringBuilder();
		try {
	    	// create a CharStream that reads from standard input
	        LLVMLexer lexer = new LLVMLexer(new ANTLRStringStream(str)) {
	        	/* (non-Javadoc)
	        	 * @see org.antlr.runtime.BaseRecognizer#emitErrorMessage(java.lang.String)
	        	 */
	        	@Override
	        	public void emitErrorMessage(String msg) {
	        		errors.append( msg +"\n");
	        	}
	        };
	        
	        // create a buffer of tokens pulled from the lexer
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        // create a parser that feeds off the tokens buffer
	        LLVMParser parser = new LLVMParser(tokens, helper);
	        // begin parsing at rule
	        ParserRuleReturnScope prog = null;
	        prog = parser.prog();
	        if (dumpLLVMGen) {
	        	System.out.println("\n"+lineize(str));
	        }
	        
	        if (!expectError) {
				if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
					System.err.println(errors);
					fail(errors.toString());
				}
			} else {
				assertTrue(parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0);
			}
	        
	        if (dumpTreeize && prog != null && prog.getTree() != null)
	        	System.out.println(((Tree) prog.getTree()).toStringTree());
	
	        if (!expectError)
	        	assertTrue("did not consume all input", tokens.index() >= tokens.size());
	
	        return prog;
		} finally {
			System.err.flush();
			System.out.flush();
			
		}
	}

	/**
	 * @param str
	 * @return
	 */
	protected String lineize(String str) {
		StringBuilder sb = new StringBuilder();
		String[] lines = str.split("\n");
		for (int l = 0; l < lines.length; l++) {
			sb.append(l+1).append(":\t").append(lines[l]).append('\n');
		}
		return sb.toString();
	}
	
	
}