import java.io.IOException;
import java.io.PushbackReader;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * This class implements a lexical analyzer for the Toy language. Tokens are
 * read in and placed into an array list so that they can be output whenever
 * needed. The lexer spots scanning tokens when the end of the file has been
 * reached.
 */
public class ToyLexer {
	private static final char EOF_CHAR = (char) -1;
	private PushbackReader 	source;
	private List<ToyToken> 	tokens;
	private Trie symTab;
	private boolean eofReached;
	
	/**
	 * Constructor
	 */
	public ToyLexer(PushbackReader source) {
		this.source = source;
		tokens = new ArrayList<ToyToken>();
		symTab = new Trie();
		eofReached = false;	
		insertKeywords(); // initialize the symbol table with the keywords
	}
	
	/**
	 * Scans the input stream and constructs the next recognized token.
	 * Whitespace and comments encountered are stripped out.
	 * The next token scanned is added to the tokens list.
	 */
	public void scanNextToken() throws IOException {
		char curr, peek;
		curr = nextUsefulChar();
		// HANDLE OPERATORS
		switch (curr) {
		
			// EOF
			case EOF_CHAR:
			tokens.add(ToyToken._eof);
			eofReached = true;
			break;
		
			// STRING CONSTANTS
			case '"':
				while ((curr = readChar()) != '"') {}
				tokens.add(ToyToken._stringconstant);
				break;
			
			// SINGLE CHAR SYMBOLS/OPERATORS
			case '+': tokens.add(ToyToken._plus); 			break;
			case '-': tokens.add(ToyToken._minus); 			break;
			case '*': tokens.add(ToyToken._multiplication); break;
			case '/': tokens.add(ToyToken._division); 		break;
			case '%': tokens.add(ToyToken._mod); 			break;
			case ';': tokens.add(ToyToken._semicolon); 		break;
			case ',': tokens.add(ToyToken._comma); 			break;
			case '.': tokens.add(ToyToken._period); 		break;
			case '(': tokens.add(ToyToken._leftparen); 		break;
			case ')': tokens.add(ToyToken._rightparen); 	break;
			case '[': tokens.add(ToyToken._leftbracket); 	break;
			case ']': tokens.add(ToyToken._rightbracket); 	break;
			case '{': tokens.add(ToyToken._leftbrace); 		break;
			case '}': tokens.add(ToyToken._rightbrace); 	break;
				
			// MULTI-CHAR OPERATORS
			case '<':
				peek = readChar();
				if (peek == '=')
					tokens.add(ToyToken._lessequal);
				else {
					pushback( peek);
					tokens.add(ToyToken._less);
				}
				break;
			case '>':
				peek = readChar();
				if (peek == '=')
					tokens.add(ToyToken._greaterequal);
				else {
					pushback( peek);
					tokens.add(ToyToken._greater);
				}
				break;
			case '=':
				peek = readChar();
				if (peek == '=')
					tokens.add(ToyToken._equal);
				else {
					pushback( peek);
					tokens.add(ToyToken._assignop);
				}
				break;
			case '!':
				peek = readChar();
				if (peek == '=')
					tokens.add(ToyToken._notequal);
				else {
					pushback( peek);
					tokens.add(ToyToken._not);
				}
				break;		
			case '&':
				peek = readChar();
				if (peek == '&')
					tokens.add(ToyToken._and);
				else {
					pushback( peek);
					tokens.add(ToyToken._ERROR);
				}
				break;		
			case '|':
				peek = readChar();
				if (peek == '|')
					tokens.add(ToyToken._or);
				else {
					pushback( peek);
					tokens.add(ToyToken._ERROR);
				}
				break;		
		}
		
		// IDENTIFIERS/KEYWORDS/BOOLEAN CONSTANTS
		if (Character.isLetter(curr)) {
			StringBuilder sb = new StringBuilder();
			sb.append(curr);
			peek = readChar();
			
			while (Character.isLetterOrDigit(peek) || peek == '_') {
				sb.append(peek);
				peek = readChar();
			}
			pushback(peek);
			String s = sb.toString();
			
			// determine if token should be id, keyword, or boolean
			switch (s) {
			case "true":
			case "false":
				tokens.add(ToyToken._booleanconstant); 	break;
			case "boolean":
				tokens.add(ToyToken._boolean); 			break;
			case "break":
				tokens.add(ToyToken._break); 			break;
			case "class":
				tokens.add(ToyToken._class); 			break;
			case "double":
				tokens.add(ToyToken._double); 			break;
			case "else":
				tokens.add(ToyToken._else); 			break;
			case "extends":
				tokens.add(ToyToken._extends); 			break;
			case "for":
				tokens.add(ToyToken._for); 				break;
			case "if":
				tokens.add(ToyToken._if); 				break;
			case "implements":
				tokens.add(ToyToken._implements); 		break;
			case "int":
				tokens.add(ToyToken._int); 				break;
			case "interface":
				tokens.add(ToyToken._interface); 		break;
			case "newarray":
				tokens.add(ToyToken._newarray); 		break;
			case "println":
				tokens.add(ToyToken._println); 			break;
			case "readln":
				tokens.add(ToyToken._readln); 			break;
			case "return":
				tokens.add(ToyToken._return); 			break;
			case "string":
				tokens.add(ToyToken._string); 			break;
			case "void":
				tokens.add(ToyToken._void); 			break;
			case "while":
				tokens.add(ToyToken._while); 			break;
			default:
				tokens.add(ToyToken._id);
					symTab.insert(s);
			}
			
		}
		// DIGITS
		else if (Character.isDigit(curr)) {
			peek = readChar();
			
			// HEX INT
			if (curr == '0' && Character.toUpperCase(peek) == 'X') {
				curr = readChar();
				if (isHexDigit(curr)) {
					tokens.add(ToyToken._intconstant);
					while (isHexDigit(curr = readChar())) {}
					pushback(curr);
				}
				else {
					tokens.add(ToyToken._intconstant);
					pushback(curr);
					pushback(peek);
				}
			}
			// DOUBLES AND DECIMAL INTS
			else {
				pushback(peek);
				while (Character.isDigit(curr))
					curr = readChar();
				
				// DOUBLE
				if (curr == '.') {
					handleDouble();
				}
				// DECIMAL INT
				else {
					pushback(curr);
					tokens.add(ToyToken._intconstant);
				}
			}
		}
	}
	
	/**
	 * Handles double constants. Method is called ONLY after a '.' has been
	 * read in from the input stream. 
	 */
	private void handleDouble() throws IOException {
		char curr;
		
		while (Character.isDigit(curr = readChar())) {}
		
		// DOUBLE WITH EXPONENT
		if (Character.toUpperCase(curr) == 'E') {
			handleExponent(curr);
		// DOUBLE WITHOUT EXPONENT	
		} else {
			tokens.add(ToyToken._doubleconstant);
			pushback(curr);
		}
	}
	
	/**
	 * Method consumes valid characters for exponent.
	 * 
	 * @param curr - character with value of 'E' or 'e'
	 * @throws IOException
	 */
	private void handleExponent(char curr) throws IOException{
		char peek1, peek2;
		
		peek1 = readChar();
		// E#...#
		if (Character.isDigit(peek1)) {
			while (Character.isDigit(peek1 = readChar())) {}
			pushback(peek1);
		// E+ or E-
		} else if (peek1 == '-' || peek1 == '+') {
			peek2 = readChar();
			// at least 1 char after +/- to be valid
			if (Character.isDigit(peek2)) {
				while (Character.isDigit(peek2 = readChar())) {}
				pushback(peek2);
			}
			// invalid exponential form, push back chars that will be used
			// for other tokens
			else {
				pushback(peek2); 	// pushback char after +/-
				pushback(peek1); 	// pushback + or -
				pushback(curr); 	// pushback e or E
			}
		// E followed by invalid char, push chars back
		} else {
			pushback(peek1);
			pushback(curr);
		}
		
		tokens.add(ToyToken._doubleconstant);
	}
	
	
	/**
	 * Checks if character is a valid hex digit
	 * 
	 * @param c - char to be checked
	 * @return - true if c is a hex digit, false otherwise
	 */
	private boolean isHexDigit(char c) {
		c = Character.toUpperCase(c);
		return Character.isDigit(c) || (c == 'A') || (c == 'B') || (c == 'C') ||
				(c == 'D') || (c == 'E') || (c == 'F');
	}
	
	/**
	 * Checks if end of file was reached
	 * 
	 * @return true is end of file reached, false otherise
	 */
	public boolean isEOF() {
		return eofReached;
	}
	
	/**
	 * Prints out the tokens to System.out
	 */
	public void dumpTokens() {
		Iterator<ToyToken> iter = tokens.iterator();
		while (iter.hasNext()) {
			ToyToken t = iter.next();
			if (t.toString().equals("carriage"))
				System.out.println();
			else if (!t.toString().equals("EOF"))
				System.out.print(t.toString() + " ");
		}
	}
	
	/**
	 * Checks if parameter c is a whitespace character
	 * 
	 * @param c - char to be checked
	 * @return true if c is whitespace, false otherwise
	 */
	private boolean isWhiteSpace(char c) {
		if (c == '\r' || c == '\n')
			tokens.add(ToyToken._carriageReturn);
		return (c == ' ') || (c == '\t') || (c == '\n') || (c == '\r');
	}
	
	
	/**
	 * Gets the first non-whitespace character for the lexer to begin token
	 * determination.
	 */
	private char nextUsefulChar() throws IOException {
		char curr = readChar();
		char peek;
		boolean usefulChar = false;
		
		do {
			while (isWhiteSpace(curr)) {
				curr = readChar();
			}
			
			// DETERMINE IF SINGLE LINE COMMENT/MULTI-LINE COMMENT/DIVISION OPERATOR
			if (curr == '/') {
				peek = readChar();
				switch (peek) {
				case '/':
					tokens.add(ToyToken._carriageReturn);
					curr = readChar();
					while (curr != '\r' && curr != '\n') {
						curr = readChar();
					}
					curr = readChar();
					break;
				case '*':
					curr = readChar();
					peek = readChar();
					while (curr != '*' || peek != '/') {
						if (curr == '\r' || curr == '\n')
							tokens.add(ToyToken._carriageReturn);
						curr = peek;
						peek = readChar();
					}
					curr = readChar();
					break;
				default:
					pushback(peek);
					usefulChar = true;
				}
			} else {
				usefulChar = true;
			}
		} while (usefulChar == false);
		return curr;
	}
	
	/**
	 * Read next character from input stream
	 * 
	 */
	private char readChar() throws IOException { return (char)source.read(); }
	
	
	/**
	 * Push back a character into the input stream
	 */
	private void pushback(char c) throws IOException { source.unread((int)c); }
	/**
	 * Insert the keywords of the Toy language into the symbol table
	 */
	private void insertKeywords() {
		symTab.insert("boolean");
		symTab.insert("break");
		symTab.insert("class");
		symTab.insert("double");
		symTab.insert("else");
		symTab.insert("extends");
		symTab.insert("false");
		symTab.insert("for");
		symTab.insert("if");
		symTab.insert("implements");
		symTab.insert("int");
		symTab.insert("interface");
		symTab.insert("newarray");
		symTab.insert("println");
		symTab.insert("readln");
		symTab.insert("return");
		symTab.insert("string");
		symTab.insert("true");
		symTab.insert("void");
		symTab.insert("while");
	}
	
	
	public void dumpSymbolTable() {
		symTab.tablePrint(15);
	}
	
	
	/**
	 * This class implements a Trie data structure to be used as a symbol
	 * table for the lexical analyzer.
	 *
	 */
	private static class Trie {
		private static final int MAX_TRANSITION = 250;
		private static final int ALPHABETIC_CHARS = 52;
		private static final int EMPTY = -1;		
		private static final char[] alphabet = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'
            , 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V'
            , 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'
            , 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v'
            , 'w', 'x', 'y', 'z'};
		
		private int 	nextFreeSpot;
		private int[] 	trieSwitch;
		private char[] 	trieSymbol;
		private int[] 	trieNext;	

		
		/**
		 * Constructor
		 */
		public Trie() {
			nextFreeSpot = 0;
			
			trieSwitch = new int [ALPHABETIC_CHARS];
			for (int i = 0; i < trieSwitch.length; i++)
				trieSwitch[i] = EMPTY;
			
			trieSymbol = new char [MAX_TRANSITION];
			trieNext = new int[MAX_TRANSITION];
			for (int i = 0; i < trieSymbol.length; i++) {
				trieSymbol[i] = ' ';
				trieNext[i] = EMPTY;
			}
		}
		
		
		//Prints out the contents of this trie in a columnated format
	    public void tablePrint (int cols) {
	        //Print alphabet and switch array
	        int i = 0;
	        while (i < alphabet.length) {
	            System.out.printf("%7s\t", "");
	            for (int j = 0; j < cols; j++) {
	                if (j + i < alphabet.length) {
	                    System.out.printf("%3c", alphabet[j + i]);
	                    System.out.print(' ');
	                }
	            }
	            System.out.print("\nswitch:\t");
	            for (int j = 0; j < cols; j++) {
	                if (j + i < alphabet.length) {
	                    System.out.printf("%3d", trieSwitch[j + i]);
	                    System.out.print(' ');
	                }
	            }
	            System.out.print("\n\n");
	            i += cols;
	        }
	        //Print out the symbols stored and ptrs
	        i = 0;
	        while (i < trieSymbol.length) {
	            System.out.printf("%7s\t", "");
	            for (int j = 0; j < cols; j++) {
	                if (j + i < trieSymbol.length) {
	                    System.out.printf("%3d", j + i);
	                    System.out.print(' ');
	                }
	            }
	            System.out.print("\nsymbol: ");
	            for (int j = 0; j < cols; j++) {
	                if (j + i < trieSymbol.length) {
	                    if (trieSymbol[j + i] != ' ') {
	                        System.out.printf("%3c", trieSymbol[j + i]);
	                        System.out.print(' ');
	                    } else {
	                        System.out.printf("%3s", "");
	                        System.out.print(' ');
	                    }

	                }
	            }
	            System.out.print("\nnext:  \t");
	            for (int j = 0; j < cols; j++) {
	                if (j + i < trieNext.length) {
	                    if (trieNext[j + i] != -1) {
	                        System.out.printf("%3d", trieNext[j + i]);
	                        System.out.print(' ');
	                    } else {
	                        System.out.printf("%3s", "");
	                        System.out.print(' ');
	                    }

	                }
	            }
	            System.out.print("\n\n");
	            i += cols;
	        }
	    }
		/**
		 * Inserts a string into the trie table
		 */
		void insert(String s) {		
			int charPos = 0;
			char c = s.charAt(charPos++);
			int switchIndex = getSwitchIndex(c);
			
			// If switch is undefined using switchIndex, create immediately
			if (trieSwitch[switchIndex] == EMPTY) {
				trieSwitch[switchIndex] = nextFreeSpot;
				
				// 1 char name (e.g. "a")
				if (s.length() == 1)
					trieSymbol[nextFreeSpot++] = '@';
				else
					create(s.substring(1), nextFreeSpot);
				return;
			}
			
			int ptr = trieSwitch[switchIndex];
			// Start with next character in string to traverse symbol table.
			// If string was one char long, '@' must be next char
			if (charPos < s.length())
				c = s.charAt(charPos++);
			else
				c = '@';
			
			boolean exit = false;
			while (!exit) {
				if (trieSymbol[ptr] == c) {
					// If c is not the terminal symbol, move to next spot
					if (c != '@') {
						ptr++;
						// get next char
						if (charPos < s.length())
							c = s.charAt(charPos++);
						else
							c = '@';
					} else {
						// c == '@' so the word already exists therefore exit.
						exit = true;
					}
				} else {	
					// trieSymbol[ptr] != c but the next spot is defined
					if (trieNext[ptr] != EMPTY)
						ptr = trieNext[ptr];
					// next spot is not defined so set it to nextFreeSpot
					// and insert what is left of the word.
					else {
						trieNext[ptr] = nextFreeSpot;
						if (s.length() == 1)
							trieSymbol[nextFreeSpot] = '@';
						else {
							create(s.substring(charPos - 1, s.length()), nextFreeSpot);
						}
						exit = true;
					}
				}
			}	
		}
		
		/**
		 * Inserts a string into an empty location in the symbol table.
		 * 
		 */
		private void create(String s, int ptr) {
			for (int i = 0; i < s.length(); i++) {
				trieSymbol[ptr++] = s.charAt(i);
			}
			trieSymbol[ptr++] = '@';
			nextFreeSpot = ptr;
		}
		
		/**
		 * Returns an index number for the switch array in the symbol table.
		 *  
		 */
		private int getSwitchIndex(char c) {
			if (Character.isUpperCase(c))
				return ((int) c) - 65;
			else
				return ((int) c) - 71;
		}
	} // end of class Trie
	
	/**
	 * Tokens are implemented using an enum. Each token is assigned a unique
	 * number to be used in the future with the syntax analyzer.
	 */
	private enum ToyToken {
		_boolean(1, "boolean"),
		_else(2, "else"),
		_implements(3, "implements"),
		_println(4, "println"),
		_void(5, "void"),
		_multiplication(6, "multiplication"),
		_lessequal(7, "lessequal"),
		_notequal(8, "notequal"),
		_assignop(9, "assignop"),
		_leftparen(10, "leftparen"),
		_leftbrace(11, "leftbrace"),
		_stringconstant(12, "stringconstant"),
		_break(13, "break"),
		_extends(14, "extends"),
		_int(15, "int"),
		_readln(16, "readln"), 
		_while(17, "while"),
		_division(18, "division"),
		_greater(19, "greater"),
		_and(20, "and"),
		_semicolon(21, "semicolon"),
		_rightparen(22, "rightparen"),
		_rightbrace(23, "rightbrace"),
		_booleanconstant(24, "booleanconstant"),
		_class(25, "class"),
		_for(26, "for"),
		_interface(27, "interface"),
		_return(28, "return"),
		_plus(29, "plus"),
		_mod(30, "mod"),
		_greaterequal(31, "greaterequal"),
		_or(32, "or"),
		_comma(33, "comma"),
		_leftbracket(34, "leftbracket"),
		_intconstant(35, "intconstant"),
		_id(36, "id"),
		_double(37, "double"),	
		_if(38, "if"),
		_newarray(39, "newarray"),
		_string(40, "string"),
		_minus(41, "minus"),
		_less(42, "less"),
		_equal(43, "equal"),
		_not(44, "not"),
		_period(45, "period"),
		_rightbracket(46, "rightbracket"),
		_doubleconstant(47, "doubleconstant"),
		_carriageReturn(48, "carriage"),
		_eof(49, "EndOfFile"),
		_ERROR(50, "ERROR_TOKEN");
		
		private final int tokenNum;
		private final String tokenString;
		
		
		ToyToken(int num, String keyword) { 
			tokenNum = num; tokenString = keyword;
		}
		
		public int getTokenNumber() { return tokenNum; }		
		public String toString() { return tokenString; }	
	} 
} 
