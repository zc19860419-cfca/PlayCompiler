package play.compiler.script.compile;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: zhangchong
 * @Description:
 */
public class PlayScriptCompilerTest {

    private boolean verbose;
    private boolean astDump;
    private PlayScriptCompiler compiler;

    @Before
    public void setUp() throws Exception {
        verbose = true;
        astDump = true;
        compiler = new PlayScriptCompiler();
    }

    @After
    public void tearDown() throws Exception {
        BlockScope.resetIndex();
    }

    /***
     * "int age = 44; for(int i = 0;i<10;i++) { age = age + 2;} int i = 8;"
     * (prog
     * 	(blockStatements
     * 		(blockStatement
     * 			(variableDeclarators
     * 				(typeType (primitiveType int))
     * 				(variableDeclarator
     * 					(variableDeclaratorId age) = (variableInitializer (expression (primary (literal (integerLiteral 44)))))
     * 				)
     * 			)
     * 		;)
     * 		(blockStatement
     * 			(statement
     * 				for (
     * 					(forControl
     * 						(forInit
     * 							(variableDeclarators
     * 								(typeType (primitiveType int))
     * 								(variableDeclarator (variableDeclaratorId i) = (variableInitializer (expression (primary (literal (integerLiteral 0))))))
     * 							)
     * 						) ;
     * 						(expression
     * 							(expression (primary i)) < (expression (primary (literal (integerLiteral 10))))
     * 						) ;
     * 						(expressionList
     * 							(expression (expression (primary i)) ++)
     * 						)
     * 					)
     * 				)
     * 				(statement
     * 					(block
     *                                                {
     * 							(blockStatements
     * 								(blockStatement
     * 									(statement
     * 										(expression
     * 											(expression (primary age)) = (expression (expression (primary age)) + (expression (primary (literal (integerLiteral 2)))))
     * 										) ;
     * 									)
     * 								)
     * 							)
     *                        }
     * 					)
     * 				)
     * 			)
     * 		)
     * 		(blockStatement
     * 			(variableDeclarators
     * 				(typeType (primitiveType int))
     * 				(variableDeclarator
     * 					(variableDeclaratorId i) = (variableInitializer (expression (primary (literal (integerLiteral 8)))))
     * 				)
     * 			) ;
     * 		)
     * 	)
     * )
     */
    @Test
    public void test_compile_with_for_scope() {
        String script = "int age = 44; for(int i = 0;i<10;i++) { age = age + 2;} int i = 8;";
        PlayScriptCompiler compiler = new PlayScriptCompiler();
        AnnotatedTree at = compiler.compile(script, verbose, astDump);

        Assert.assertFalse(at.hasCompilationError());
    }

    /**
     * (prog (blockStatements (blockStatement (variableDeclarators (typeType (primitiveType int)) (variableDeclarator (variableDeclaratorId b) = (variableInitializer (expression (primary (literal (integerLiteral 10))))))) ;) (blockStatement (functionDeclaration (typeTypeOrVoid (typeType (primitiveType int))) myfunc (formalParameters ( (formalParameterList (formalParameter (typeType (primitiveType int)) (variableDeclaratorId a))) )) (functionBody (block { (blockStatements (blockStatement (statement return (expression (expression (expression (primary a)) + (expression (primary b))) + (expression (primary (literal (integerLiteral 3))))) ;))) })))) (blockStatement (statement (expression (functionCall myfunc ( (expressionList (expression (primary (literal (integerLiteral 2))))) ))) ;))))
     */
    @Test
    public void test_compile_with_func_scope() {
        String script = "int b= 10; int myfunc(int a) {return a+b+3;} myfunc(2);";
        AnnotatedTree at = compiler.compile(script, verbose, astDump);

        Assert.assertFalse(at.hasCompilationError());
    }

    @Test
    public void test_compile_with_script_simple() throws Exception {
        String script = readScript("../TestData/simple.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);

        Assert.assertFalse(at.hasCompilationError());
    }

    @Test
    public void test_compile_with_script_ClassTest() throws Exception {
        String script = readScript("../TestData/ClassTest.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);

        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();
        final String expected = "Block block1\n" +
                "\tClass Mammal\n" +
                "\t\tFunction Mammal\n" +
                "\t\t\tVariable str -> String\n" +
                "\t\tFunction speak\n" +
                "\t\tVariable name -> String\n" +
                "\tClass Bird\n" +
                "\t\tFunction fly\n" +
                "\t\tVariable speed -> Integer\n" +
                "\tVariable mammal -> Class Mammal\n" +
                "\tVariable bird -> Class Bird";
        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    @Test
    public void test_compile_with_script_mammal() throws Exception {
        String script = readScript("../TestData/mammal.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);

        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();
        final String expected = "Block block1\n" +
                "\tClass Mammal\n" +
                "\t\tFunction canSpeak\n" +
                "\t\tFunction speak\n" +
                "\tClass Cow\n" +
                "\t\tFunction speak\n" +
                "\tClass Sheep\n" +
                "\t\tFunction speak\n" +
                "\tVariable a -> Class Mammal\n" +
                "\tVariable b -> Class Mammal";
        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    @Test
    public void test_compile_with_script_loop() throws Exception {
        String script = readScript("../TestData/loop.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);
        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();

        String expected = "Block block1\n" +
                "\tBlock block2\n" +
                "\tBlock block3\n" +
                "\t\tBlock block4\n" +
                "\t\tVariable i -> Integer\n" +
                "\tBlock block5\n" +
                "\tBlock block6\n" +
                "\t\tBlock block7\n" +
                "\t\tVariable i -> Integer\n" +
                "\tBlock block8\n" +
                "\t\tBlock block9\n" +
                "\t\t\tBlock block10\n" +
                "\t\t\tVariable j -> Integer\n" +
                "\t\tBlock block11\n" +
                "\tVariable i -> Integer\n" +
                "\tVariable a -> Integer";
        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    @Test
    public void test_compile_with_script_expressions() throws Exception {
        String script = readScript("../TestData/expressions.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);
        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();
        System.out.println(scopeTreeString);

        Object result = compiler.Execute(at);
        System.out.println(result);
//        String expected = "Block block1\n" +
//                "\tVariable a -> Integer\n" +
//                "\tVariable b -> Integer\n" +
//                "\tVariable c -> Boolean\n" +
//                "\tVariable str1 -> String\n" +
//                "\tVariable str2 -> String\n" +
//                "\tVariable str3 -> null";
//        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    @Test
    public void test_compile_with_script_expressions1() throws Exception {
        String script = readScript("../TestData/expressions1.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);
        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();
        System.out.println(scopeTreeString);

        Object result = compiler.Execute(at);
        System.out.println(result);
    }

    @Test
    public void test_compile_with_script_FirstClassFunction() throws Exception {
        String script = readScript("../TestData/FirstClassFunction.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);
        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();

        String expected = "Block block1\n" +
                "\tFunction foo\n" +
                "\t\tVariable a -> Integer\n" +
                "\tFunction bar\n" +
                "\t\tVariable fun -> FunctionType\n" +
                "\t\tVariable b -> Integer\n" +
                "\tVariable a -> FunctionType\n" +
                "\tVariable b -> FunctionType";
        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    @Test
    public void test_compile_with_script_function() throws Exception {
        String script = readScript("../TestData/function.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);
        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();

        String expected = "Block block1\n" +
                "\tFunction foo\n" +
                "\t\tVariable a -> Integer\n" +
                "\tFunction bar\n" +
                "\t\tBlock block2\n" +
                "\t\t\tBlock block3\n" +
                "\t\t\t\tBlock block4\n" +
                "\t\t\t\t\tVariable d -> Integer\n" +
                "\t\t\tVariable i -> Integer\n" +
                "\t\tBlock block5\n" +
                "\t\t\tBlock block6\n" +
                "\t\t\t\tBlock block7\n" +
                "\t\t\tVariable j -> Integer\n" +
                "\t\tVariable a -> Integer";
        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    @Test
    public void test_compile_with_script_LinkedList() throws Exception {
        String script = readScript("../TestData/LinkedList.play");
        AnnotatedTree at = compiler.compile(script, verbose, astDump);
        Assert.assertFalse(at.hasCompilationError());

        final String scopeTreeString = at.getScopeTreeString();

        String expected = "Block block1\n" +
                "\tClass ListNode\n" +
                "\t\tFunction ListNode\n" +
                "\t\t\tVariable v -> Integer\n" +
                "\t\tVariable value -> Integer\n" +
                "\t\tVariable next -> Class ListNode\n" +
                "\tClass LinkedList\n" +
                "\t\tFunction add\n" +
                "\t\t\tBlock block2\n" +
                "\t\t\tBlock block3\n" +
                "\t\t\tVariable value -> Integer\n" +
                "\t\t\tVariable node -> Class ListNode\n" +
                "\t\tFunction dump\n" +
                "\t\t\tBlock block4\n" +
                "\t\t\tVariable node -> Class ListNode\n" +
                "\t\tFunction map\n" +
                "\t\t\tBlock block5\n" +
                "\t\t\t\tVariable newValue -> Integer\n" +
                "\t\t\tVariable fun -> FunctionType\n" +
                "\t\t\tVariable node -> Class ListNode\n" +
                "\t\t\tVariable newList -> Class LinkedList\n" +
                "\t\tVariable start -> Class ListNode\n" +
                "\t\tVariable end -> Class ListNode\n" +
                "\tFunction square\n" +
                "\t\tVariable value -> Integer\n" +
                "\tFunction addOne\n" +
                "\t\tVariable value -> Integer\n" +
                "\tVariable list -> Class LinkedList\n" +
                "\tVariable list2 -> Class LinkedList\n" +
                "\tVariable list3 -> Class LinkedList";
        Assert.assertEquals(expected, scopeTreeString.trim());
    }

    private String readScript(String s) throws IOException {
        final List<String> lines = FileUtils.readLines(new File(s), StandardCharsets.UTF_8);
        final StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }
}