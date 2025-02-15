import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SyntacticAnalyser {

	private static Map<TreeNode.Label, Map<Token.TokenType, ArrayList<Symbol>>> parseTable;

	public static ParseTree parse(List<Token> tokens) throws SyntaxException {
		// Create output tree and stack
		int index = 0;
		ParseTree result = new ParseTree();
		ArrayDeque<TreeNode> varStack = new ArrayDeque<TreeNode>();
		TreeNode currentNode;
		parseTable = new HashMap<>();
		PopulateParseTable();

		// Push starting symbol S = prog to stack
		TreeNode root = new TreeNode(TreeNode.Label.prog, null);
		result.setRoot(root);
		varStack.push(root);

		// Perform PDA process
		while (!varStack.isEmpty() && index < tokens.size()) {
			currentNode = varStack.pop();
			Symbol currentStackSymbol = currentNode.getLabel();

			// Top of the stack is a terminal -> we have to match this with the current token
			// if it doesn't match, throw an exception
			if (currentNode.getLabel() == TreeNode.Label.terminal) {
				Token.TokenType currentVarAsTokentype = currentNode.getToken().get().getType();
				currentNode.getToken().get().setValue(tokens.get(index).getValue().get());
				if (currentVarAsTokentype == tokens.get(index).getType()) {
					index++;
				}
				else {
					Token.TokenType actualType = tokens.get(index).getType();
					throw new SyntaxException("Mismatch between expected terminal " + currentVarAsTokentype.toString() + " and actual: " + actualType);
				}
			}

			// Top of the stack is a non terminal -> lookup in the table what needs to be 
			// pushed to the stack
			else {
				TreeNode.Label currentStackSymbolAsLabel = (TreeNode.Label)currentStackSymbol;
				if (currentStackSymbolAsLabel == TreeNode.Label.epsilon) {
					continue;
				}
				Token.TokenType currentTerminal = tokens.get(index).getType();

				// Try to lookup what symbols need to be pushed- if there is no rule, we have
				// entered an error state!
				try {
					List<Symbol> symbolsToPush = parseTable.get(currentStackSymbolAsLabel).get(currentTerminal);
					for (Symbol symbolToPush : symbolsToPush) {
						if (symbolToPush.isVariable()) {
							TreeNode newNode = new TreeNode((TreeNode.Label)symbolToPush, currentNode);
							currentNode.addChildToStart(newNode);
							varStack.push(newNode);
						}
						else {
							String tokenValue = tokens.get(index).getValue().get();
							TreeNode newNode = new TreeNode(TreeNode.Label.terminal, new Token((Token.TokenType)symbolToPush, tokenValue), currentNode);
							currentNode.addChildToStart(newNode);
							varStack.push(newNode);
						}
					}
				}
				catch (Exception e) {
					throw new SyntaxException("Entry not in table, cant have this production rule " + currentStackSymbolAsLabel + " with this terminal " + currentTerminal + " ");
				}
			}
		}

		// If we have not reached the end of the token list, we have unexpected tokens
		if (index != tokens.size()) {
			throw new SyntaxException("There's still stuff left in the token list!: " + index + " | " + (tokens.size() - 1));
		}

		// If we have consumed all characters and emptied the stack we are lacking tokens
		if (!varStack.isEmpty()) {
			throw new SyntaxException("Run out of tokens but there's still symbols in the stack, substitution to be done!");
		}

		return result;
	}

	private static void PopulateParseTable() {
		for (TreeNode.Label label : TreeNode.Label.values()) {
            parseTable.put(label, new HashMap<>());
        }

		// prog
		parseTable.get(TreeNode.Label.prog).put(Token.TokenType.PUBLIC, new ArrayList<Symbol>(Arrays.asList(
			Token.TokenType.RBRACE,
			Token.TokenType.RBRACE,
			TreeNode.Label.los,
			Token.TokenType.LBRACE,
			Token.TokenType.RPAREN,
			Token.TokenType.ARGS,
			Token.TokenType.STRINGARR,
			Token.TokenType.LPAREN,
			Token.TokenType.MAIN,
			Token.TokenType.VOID,
			Token.TokenType.STATIC,
			Token.TokenType.PUBLIC,
			Token.TokenType.LBRACE,
			Token.TokenType.ID,
			Token.TokenType.CLASS,
			Token.TokenType.PUBLIC
		)));

		// los
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.WHILE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.FOR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.IF, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.TYPE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.PRINT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.los, TreeNode.Label.stat)));
		parseTable.get(TreeNode.Label.los).put(Token.TokenType.RBRACE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// stat
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.SEMICOLON, TreeNode.Label.assign)));
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.PRINT, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.SEMICOLON, TreeNode.Label.print)));
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.TYPE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.SEMICOLON, TreeNode.Label.decl)));
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.IF, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.ifstat)));
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.FOR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.forstat)));
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.WHILE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.whilestat)));
		parseTable.get(TreeNode.Label.stat).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.SEMICOLON)));

		// while
		parseTable.get(TreeNode.Label.whilestat).put(Token.TokenType.WHILE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.RBRACE, TreeNode.Label.los, Token.TokenType.LBRACE, Token.TokenType.RPAREN, TreeNode.Label.boolexpr, TreeNode.Label.relexpr, Token.TokenType.LPAREN, Token.TokenType.WHILE)));

		// for
		parseTable.get(TreeNode.Label.forstat).put(Token.TokenType.FOR, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.RBRACE, TreeNode.Label.los, Token.TokenType.LBRACE, Token.TokenType.RPAREN, TreeNode.Label.forarith, Token.TokenType.SEMICOLON, TreeNode.Label.boolexpr, TreeNode.Label.relexpr, Token.TokenType.SEMICOLON, TreeNode.Label.forstart, Token.TokenType.LPAREN, Token.TokenType.FOR)));

		// for start
		parseTable.get(TreeNode.Label.forstart).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.assign)));
		parseTable.get(TreeNode.Label.forstart).put(Token.TokenType.TYPE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.decl)));
		parseTable.get(TreeNode.Label.forstart).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// for arith
		parseTable.get(TreeNode.Label.forarith).put(Token.TokenType.LBRACE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr)));
		parseTable.get(TreeNode.Label.forarith).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr)));
		parseTable.get(TreeNode.Label.forarith).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr)));
		parseTable.get(TreeNode.Label.forarith).put(Token.TokenType.RPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// if
		parseTable.get(TreeNode.Label.ifstat).put(Token.TokenType.IF, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.elseifstat, Token.TokenType.RBRACE, TreeNode.Label.los, Token.TokenType.LBRACE, Token.TokenType.RPAREN, TreeNode.Label.boolexpr, TreeNode.Label.relexpr, Token.TokenType.LPAREN, Token.TokenType.IF)));

		// else if
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.TYPE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.PRINT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.FOR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.WHILE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.RBRACE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.elseifstat).put(Token.TokenType.ELSE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.elseifstat, Token.TokenType.RBRACE, TreeNode.Label.los, Token.TokenType.LBRACE, TreeNode.Label.elseorelseif)));

		// else or else if
		parseTable.get(TreeNode.Label.elseorelseif).put(Token.TokenType.ELSE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.possif, Token.TokenType.ELSE)));

		// poss if
		parseTable.get(TreeNode.Label.possif).put(Token.TokenType.IF, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.RPAREN, TreeNode.Label.boolexpr, TreeNode.Label.relexpr, Token.TokenType.LPAREN, Token.TokenType.IF)));
		parseTable.get(TreeNode.Label.possif).put(Token.TokenType.LBRACE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// assign
		parseTable.get(TreeNode.Label.assign).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.expr, Token.TokenType.ASSIGN, Token.TokenType.ID)));

		// decl
		parseTable.get(TreeNode.Label.decl).put(Token.TokenType.TYPE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.possassign, Token.TokenType.ID, TreeNode.Label.type)));

		// pos assign
		parseTable.get(TreeNode.Label.possassign).put(Token.TokenType.ASSIGN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.expr, Token.TokenType.ASSIGN)));
		parseTable.get(TreeNode.Label.possassign).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// print
		parseTable.get(TreeNode.Label.print).put(Token.TokenType.PRINT, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.RPAREN, TreeNode.Label.printexpr, Token.TokenType.LPAREN, Token.TokenType.PRINT)));

		// type
		parseTable.get(TreeNode.Label.type).put(Token.TokenType.TYPE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.TYPE)));

		// expr
		parseTable.get(TreeNode.Label.expr).put(Token.TokenType.TRUE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.expr).put(Token.TokenType.FALSE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.expr).put(Token.TokenType.LBRACE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.expr).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.expr).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.expr).put(Token.TokenType.SQUOTE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.charexpr)));

		// char expr
		parseTable.get(TreeNode.Label.charexpr).put(Token.TokenType.SQUOTE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.SQUOTE, Token.TokenType.CHARLIT, Token.TokenType.SQUOTE)));

		// bool expr
		parseTable.get(TreeNode.Label.boolexpr).put(Token.TokenType.AND, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr, TreeNode.Label.boolop)));
		parseTable.get(TreeNode.Label.boolexpr).put(Token.TokenType.OR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr, TreeNode.Label.boolop)));
		parseTable.get(TreeNode.Label.boolexpr).put(Token.TokenType.EQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr, TreeNode.Label.boolop)));
		parseTable.get(TreeNode.Label.boolexpr).put(Token.TokenType.NEQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr, TreeNode.Label.boolop)));
		parseTable.get(TreeNode.Label.boolexpr).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.boolexpr).put(Token.TokenType.RPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// bool op
		parseTable.get(TreeNode.Label.boolop).put(Token.TokenType.EQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.booleq)));
		parseTable.get(TreeNode.Label.boolop).put(Token.TokenType.NEQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.booleq)));
		parseTable.get(TreeNode.Label.boolop).put(Token.TokenType.AND, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boollog)));
		parseTable.get(TreeNode.Label.boolop).put(Token.TokenType.OR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boollog)));

		// bool eq
		parseTable.get(TreeNode.Label.booleq).put(Token.TokenType.EQUAL, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.EQUAL)));
		parseTable.get(TreeNode.Label.booleq).put(Token.TokenType.NEQUAL, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.NEQUAL)));

		// bool log
		parseTable.get(TreeNode.Label.booleq).put(Token.TokenType.AND, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.AND)));
		parseTable.get(TreeNode.Label.booleq).put(Token.TokenType.OR, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.OR)));

		// rel expr
		parseTable.get(TreeNode.Label.relexpr).put(Token.TokenType.TRUE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.TRUE)));
		parseTable.get(TreeNode.Label.relexpr).put(Token.TokenType.FALSE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.FALSE)));
		parseTable.get(TreeNode.Label.relexpr).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.relexprprime, TreeNode.Label.arithexpr)));
		parseTable.get(TreeNode.Label.relexpr).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.relexprprime, TreeNode.Label.arithexpr)));
		parseTable.get(TreeNode.Label.relexpr).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.relexprprime, TreeNode.Label.arithexpr)));

		// rel expr'
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.GE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr, TreeNode.Label.relop)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.GT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr, TreeNode.Label.relop)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.LE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr, TreeNode.Label.relop)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.LT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexpr, TreeNode.Label.relop)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.AND, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.OR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.EQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.NEQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.relexprprime).put(Token.TokenType.RPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// rel op
		parseTable.get(TreeNode.Label.relop).put(Token.TokenType.GE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.GE)));
		parseTable.get(TreeNode.Label.relop).put(Token.TokenType.GT, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.GT)));
		parseTable.get(TreeNode.Label.relop).put(Token.TokenType.LE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.LE)));
		parseTable.get(TreeNode.Label.relop).put(Token.TokenType.LT, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.LT)));

		// arith expr
		parseTable.get(TreeNode.Label.arithexpr).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexprprime, TreeNode.Label.term)));
		parseTable.get(TreeNode.Label.arithexpr).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexprprime, TreeNode.Label.term)));
		parseTable.get(TreeNode.Label.arithexpr).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexprprime, TreeNode.Label.term)));

		// arith expr'
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.PLUS, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexprprime, TreeNode.Label.term, Token.TokenType.PLUS)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.MINUS, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.arithexprprime, TreeNode.Label.term, Token.TokenType.MINUS)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.GE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.GT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.LE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.LT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.AND, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.OR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.EQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.NEQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.arithexprprime).put(Token.TokenType.RPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// term
		parseTable.get(TreeNode.Label.term).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.termprime, TreeNode.Label.factor)));
		parseTable.get(TreeNode.Label.term).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.termprime, TreeNode.Label.factor)));
		parseTable.get(TreeNode.Label.term).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.termprime, TreeNode.Label.factor)));

		// term'
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.TIMES, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.termprime, TreeNode.Label.factor, Token.TokenType.TIMES)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.DIVIDE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.termprime, TreeNode.Label.factor, Token.TokenType.DIVIDE)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.MOD, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.termprime, TreeNode.Label.factor, Token.TokenType.MOD)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.PLUS, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.MINUS, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.GE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.GT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.LE, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.LT, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.OR, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.AND, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.EQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.NEQUAL, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.RPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));
		parseTable.get(TreeNode.Label.termprime).put(Token.TokenType.SEMICOLON, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.epsilon)));

		// factor
		parseTable.get(TreeNode.Label.factor).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.RPAREN, TreeNode.Label.arithexpr, Token.TokenType.LPAREN)));
		parseTable.get(TreeNode.Label.factor).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.ID)));
		parseTable.get(TreeNode.Label.factor).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.NUM)));

		// print expr
		parseTable.get(TreeNode.Label.printexpr).put(Token.TokenType.LPAREN, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.printexpr).put(Token.TokenType.ID, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.printexpr).put(Token.TokenType.NUM, new ArrayList<Symbol>(Arrays.asList(TreeNode.Label.boolexpr, TreeNode.Label.relexpr)));
		parseTable.get(TreeNode.Label.printexpr).put(Token.TokenType.DQUOTE, new ArrayList<Symbol>(Arrays.asList(Token.TokenType.DQUOTE, Token.TokenType.STRINGLIT, Token.TokenType.DQUOTE)));
	}
}


