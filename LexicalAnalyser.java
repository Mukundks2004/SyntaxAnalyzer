import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LexicalAnalyser {

	public static List<Token> analyse(String sourceCode) throws LexicalException {
		
		//Turn the input String into a list of Tokens!
		String endToken = "{}();\"' ";
		List<Token> result = new ArrayList<Token>();

		// 0 means a word is an ID
		// 1 means a word is a STRLIT
		// 2 means a word is a CHARLIT

		int expectWord = 0;

		int index = 0;
		while (index < sourceCode.length()) {
			StringBuilder sb = new StringBuilder();

			if (sourceCode.charAt(index) == ' ') {
				while (index < sourceCode.length() && sourceCode.charAt(index) == ' ') {
					sb.append(sourceCode.charAt(index));
					index++;
				}
			}
			else if (sourceCode.charAt(index) == '}') {
				result.add(tokenFromString("}", expectWord).get());
				index++;
			}
			else if (sourceCode.charAt(index) == '{') {
				result.add(tokenFromString("{", expectWord).get());
				index++;
			}
			else if (sourceCode.charAt(index) == ')') {
				result.add(tokenFromString(")", expectWord).get());
				index++;
			}
			else if (sourceCode.charAt(index) == '(') {
				result.add(tokenFromString("(", expectWord).get());
				index++;
			}
			else if (sourceCode.charAt(index) == ';') {
				result.add(tokenFromString(";", expectWord).get());
				index++;
			}
			else if (sourceCode.charAt(index) == '"') {
				if (expectWord == 1) {
					expectWord = 0;
				}
				else {
					expectWord = 1;
				}
				result.add(new Token(Token.TokenType.DQUOTE, "\""));
				index++;
			}
			else if (sourceCode.charAt(index) == '\'') {
				if (expectWord == 2) {
					expectWord = 0;
				}
				else {
					expectWord = 2;
				}
				result.add(new Token(Token.TokenType.SQUOTE, "'"));
				index++;
			}
			else {

				while (index < sourceCode.length() && endToken.indexOf(sourceCode.charAt(index)) == -1) {
					sb.append(sourceCode.charAt(index));
					index++;
				}

				Optional<Token> tokenfromString = tokenFromString(sb.toString(), expectWord);
				if (!tokenfromString.isPresent()) {
					throw new LexicalException("Bad string: " + sb.toString());
				}

				result.add(tokenfromString.get());
			}
		}
		
		return result;
	}

	private static Optional<Token> tokenFromString(String t, int wordState) {
		Optional<Token.TokenType> type = tokenTypeOf(t, wordState);
		if (type.isPresent())
			return Optional.of(new Token(type.get(), t));
		return Optional.empty();
	}

	private static Optional<Token.TokenType> tokenTypeOf(String t, int wordState) {
		if (wordState == 1) {
			return Optional.of(Token.TokenType.STRINGLIT);
		}
		else if (wordState == 2) {
			return Optional.of(Token.TokenType.CHARLIT);
		}
		switch (t) {
		case "public":
			return Optional.of(Token.TokenType.PUBLIC);
		case "class":
			return Optional.of(Token.TokenType.CLASS);
		case "static":
			return Optional.of(Token.TokenType.STATIC);
		case "main":
			return Optional.of(Token.TokenType.MAIN);
		case "{":
			return Optional.of(Token.TokenType.LBRACE);
		case "void":
			return Optional.of(Token.TokenType.VOID);
		case "(":
			return Optional.of(Token.TokenType.LPAREN);
		case "String[]":
			return Optional.of(Token.TokenType.STRINGARR);
		case "args":
			return Optional.of(Token.TokenType.ARGS);
		case ")":
			return Optional.of(Token.TokenType.RPAREN);
		case "int":
		case "char":
		case "boolean":
			return Optional.of(Token.TokenType.TYPE);
		case "=":
			return Optional.of(Token.TokenType.ASSIGN);
		case ";":
			return Optional.of(Token.TokenType.SEMICOLON);
		case "if":
			return Optional.of(Token.TokenType.IF);
		case "for":
			return Optional.of(Token.TokenType.FOR);
		case "while":
			return Optional.of(Token.TokenType.WHILE);
		case "==":
			return Optional.of(Token.TokenType.EQUAL);
		case "+":
			return Optional.of(Token.TokenType.PLUS);
		case "-":
			return Optional.of(Token.TokenType.MINUS);
		case "*":
			return Optional.of(Token.TokenType.TIMES);
		case "/":
			return Optional.of(Token.TokenType.DIVIDE);
		case "%":
			return Optional.of(Token.TokenType.MOD);
		case "}":
			return Optional.of(Token.TokenType.RBRACE);
		case "else":
			return Optional.of(Token.TokenType.ELSE);
		case "System.out.println":
			return Optional.of(Token.TokenType.PRINT);
		case "||":
			return Optional.of(Token.TokenType.OR);
		case "&&":
			return Optional.of(Token.TokenType.AND);
		case "true":
			return Optional.of(Token.TokenType.TRUE);
		case "false":
			return Optional.of(Token.TokenType.FALSE);
		}

		if (t.matches("\\d+"))
			return Optional.of(Token.TokenType.NUM);
		if (Character.isAlphabetic(t.charAt(0)) && t.matches("[\\d|\\w]+")) {
			return Optional.of(Token.TokenType.ID);
		}
		return Optional.empty();
	}

}

