package play.compiler.simple;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import play.compiler.simple.generate.PlayScriptLexer;
import play.compiler.simple.generate.PlayScriptParser;

import java.io.IOException;

/**
 * @Author: zhangchong
 * @Description:
 */
public class SimpleScript {
    public static void main(String args[]) {
//        testReadFromFile();
        testReadFromString();

    }

    private static void testReadFromString() {
        //词法分析
        PlayScriptLexer lexer = null;
        lexer = new PlayScriptLexer(CharStreams.fromString("2+6/3"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        //语法分析
        PlayScriptParser parser = new PlayScriptParser(tokens);
        ParseTree tree = parser.expression();

        //打印语法树
        System.out.println(tree.toStringTree(parser));

        //解释执行
        SimpleAstEvaluator visitor = new SimpleAstEvaluator();
        Integer result = visitor.visit(tree);
        System.out.println("\nValue of : ");
        System.out.println(result);
    }

    private static void testReadFromFile() {
        try {
            //词法分析
            PlayScriptLexer lexer = null;
            lexer = new PlayScriptLexer(CharStreams.fromFileName("TestData/simple.play"));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            //语法分析
            PlayScriptParser parser = new PlayScriptParser(tokens);
            ParseTree tree = parser.expression();

            //打印语法树
            System.out.println(tree.toStringTree(parser));

            //解释执行
            SimpleAstEvaluator visitor = new SimpleAstEvaluator();
            Integer result = visitor.visit(tree);
            System.out.println("\nValue of : ");
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
