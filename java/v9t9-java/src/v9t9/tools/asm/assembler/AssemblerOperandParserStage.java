/**
 * 
 */
package v9t9.tools.asm.assembler;

import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;
import v9t9.tools.asm.assembler.operand.hl.BinaryOperand;
import v9t9.tools.asm.assembler.operand.hl.NumberOperand;
import v9t9.tools.asm.assembler.operand.hl.PcRelativeOperand;
import v9t9.tools.asm.assembler.operand.hl.StringOperand;
import v9t9.tools.asm.assembler.operand.hl.SymbolOperand;
import v9t9.tools.asm.assembler.operand.hl.UnaryOperand;

/**
 * Parse operands for an assembler.  This handles expressions, symbols,
 * and forward references.
 * @author ejs
 *
 */
public abstract class AssemblerOperandParserStage implements IOperandParserStage {

	protected final Assembler assembler;
	protected AssemblerTokenizer tokenizer;

	public AssemblerOperandParserStage(Assembler assembler) {
		this.assembler = assembler;
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.IOperandParserStage#parse(java.lang.String)
	 */
	public AssemblerOperand parse(AssemblerTokenizer tokenizer) throws ParseException {
		this.tokenizer = tokenizer;

		AssemblerOperand op = parseExpr();
		return op;
	}

	/*
	 * term ExprRest 
	 * | '+' term ExprRest
	 * | '-' term ExprRest
	 */
	protected AssemblerOperand parseExpr() throws ParseException {
		AssemblerOperand op = parseExprRest(parseTerm());
		return op;
	}

	private AssemblerOperand parseExprRest(AssemblerOperand term) throws ParseException {
		int t = tokenizer.nextToken();
		if (t == '+' || t == '-') {
			AssemblerOperand op = parseExprRest(new BinaryOperand(t, term, parseTerm()));
			return op;
		}
		tokenizer.pushBack();
		return term;
	}

	private AssemblerOperand parseTerm() throws ParseException {
		AssemblerOperand op = parseTermRest(parseFactor());
		return op;
	}

	private AssemblerOperand parseTermRest(AssemblerOperand factor) throws ParseException {
		int t = tokenizer.nextToken();
		if (t == '*' || t == '/') {
			AssemblerOperand op = parseExprRest(new BinaryOperand(t, factor, parseFactor()));
			return op;
		}
		tokenizer.pushBack();
		return factor;
	}

	protected abstract AssemblerOperand parseTargetSpecificOperand(int token) throws ParseException;
	
	protected AssemblerOperand parseFactor() throws ParseException {
		int t = tokenizer.nextToken();
		AssemblerOperand top = parseTargetSpecificOperand(t);
		if (top != null)
			return top;
		
		switch (t) {
		case '+': {
			AssemblerOperand op = parseFactor();
			if (op instanceof NumberOperand)
				return op;
			throw new ParseException("Suspicious use of + for " + op);
		}
		case '-': { 
			AssemblerOperand op = parseFactor();
			return new UnaryOperand('-', op);
		}
		case '$':
			// either expr like "$ + foo" or "$ 2 +"
			return parseJumpTarget();
		case '(': {
			AssemblerOperand op = parseExpr();
			t = tokenizer.nextToken();
			if (t != ')')
				throw new ParseException("Expected close paren");
			return op;
		}
		case AssemblerTokenizer.NUMBER:
			return makeNumber(tokenizer.getNumber());
		case AssemblerTokenizer.CHAR: {
			String ch = tokenizer.getString();
			if (ch.length() == 1)
				return makeNumber((char)ch.charAt(0));
			else if (ch.length() == 2)
				return makeNumber((ch.charAt(0) << 8) | (ch.charAt(1) & 0xff));
			else
				throw new ParseException("Char literal is wrong length: " + ch);
			}
		case AssemblerTokenizer.ID:
			Symbol symbol = assembler.referenceSymbol(tokenizer.getString());
			if (symbol instanceof Equate) {
				return makeNumber(((Equate) symbol).getValue());
			}
			return new SymbolOperand(symbol);
		case AssemblerTokenizer.STRING:
			return new StringOperand(tokenizer.getString());
			
		case AssemblerTokenizer.EOF:
			throw new ParseException("Unexpected end of line");
		}

		throw new ParseException("Unknown token: " + tokenizer.currentToken());

	}

	private AssemblerOperand parseJumpTarget() throws ParseException {
		int t = tokenizer.nextToken();
		if (t == AssemblerTokenizer.NUMBER) {
			String target = tokenizer.currentToken();
			Symbol symbol;
			t = tokenizer.nextToken();
			String labelName = "$" + target;
			if (t == '+') {
				symbol = assembler.findForwardLocalLabel(labelName);
			} else {
				symbol = assembler.findBackwardLocalLabel(labelName);
				// back ref: use previous one
				//symbol = assembler.getSymbolTable().findSymbol(labelName);
				//if (symbol == null)
				//	throw new ParseException("No previous label " + labelName);
			}
			return new SymbolOperand(symbol);
		} else {
			tokenizer.pushBack();
			return new PcRelativeOperand();
		}
	}

	protected AssemblerOperand makeNumber(int i) {
		return new NumberOperand(i);
	}


}
