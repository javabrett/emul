/**
 * 
 */
package v9t9.tools.asm;

import java.io.IOException;

import v9t9.tools.llinst.ParseException;

/**
 * Tokenize operands for assembler
 * @author ejs
 *
 */
public class AssemblerTokenizer {
	private int curtoken;
	private String image;
	private int number;
	private boolean pushedBack;
	private TokenReader reader;

	public static final int EOF = 0;
	public static final int NUMBER = 1;
	public static final int ID = 2;
	public static final int CHAR = 3;
	public static final int STRING = 4;
	
	public AssemblerTokenizer(String string) {
		this(new StringTokenReader(string));
	}
	public AssemblerTokenizer(TokenReader reader) {
		this.reader = reader;
	}

	public int nextToken() throws ParseException {
		if (pushedBack) {
			pushedBack = false;
			return curtoken;
		}
		try {
			curtoken = parseToken();
		} catch (IOException e) {
			curtoken = EOF;
		}
		return curtoken;
	}
	
	private int parseToken() throws IOException, ParseException {
		int ch = reader.read();
		
		while (ch != -1) {
			if (!Character.isWhitespace(ch))
				break;
			ch = reader.read();
		}
		if (ch == -1)
			return EOF;
		
		image = "";
		number = 0;
		
		if (ch >= '0' && ch <= '9') {
			do {
				image += (char) ch;
				ch = reader.read();
			} while (ch != -1 && ch >= '0' && ch <= '9');
			if (ch != -1) reader.unread();
			number = Integer.parseInt(image);
			return NUMBER;
		} else if (ch == '>') {
			do {
				image += (char)ch;
				ch = reader.read();
				if (ch >= '0' && ch <= '9')
					number = (number << 4) | (ch - '0');
				else if (ch >= 'A' && ch <= 'F')
					number = (number << 4) | (ch - 'A' + 10);
				else if (ch >= 'a' && ch <= 'f')
					number = (number << 4) | (ch - 'a' + 10);
				else {
					break;
				}
			} while (ch != -1);
			if (ch != -1) reader.unread();
			return NUMBER;
		} else if (isLetterChar(ch)) {
			do {
				image += (char) ch;
				ch = reader.read();
			} while (ch != -1 && isIdentiferChar(ch));
			if (ch != -1) reader.unread();
			return ID;
		} else if (ch == '\'' || ch == '\"') {
			int end = ch;
			while ((ch = reader.read()) != -1 && ch != end) {
				image += (char) ch;
			}
			if (ch == -1)
				throw new ParseException("Unterminated constant: " + (char)ch);
			return end == '\'' ? CHAR : STRING;
		} else {
			image += (char) ch;
			return ch;
		}
	}

	private boolean isIdentiferChar(int ch) {
		return isLetterChar(ch) || (ch >= '0' && ch <= '9');
	}
	private boolean isLetterChar(int ch) {
		return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
	}
	
	public int getNumber() {
		return number;
	}
	
	public String getId() {
		return image;
	}
	
	public String getString() {
		return image;
	}
	
	public void pushBack() {
		if (curtoken != 0) {
			/*
			try {
				reader.reset();
			} catch (IOException e) {
				throw (IllegalStateException) new IllegalStateException().initCause(e);
			}*/
			pushedBack = true;
		}
	}

	public String currentToken() {
		return image;
	}

	public int currentTokenType() {
		return curtoken;
	}
	public int getPos() {
		return reader.getPos();
	}
	public void setPos(int pos) {
		reader.setPos(pos);
	}

}
