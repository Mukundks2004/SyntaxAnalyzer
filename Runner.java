import java.util.List;

public class Runner {

	public static void main(String[] args) {
		try {
			List<Token> results = LexicalAnalyser.analyse("public class Test { public static void main(String[] args){ }}");
			System.out.println(results);
			ParseTree tree = SyntacticAnalyser.parse(results);
			System.out.println(tree);
		} catch (LexicalException e) {
			e.printStackTrace();
		} catch (SyntaxException e) {
			e.printStackTrace();
		}
	}
}
