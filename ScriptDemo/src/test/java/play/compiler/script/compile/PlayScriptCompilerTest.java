package play.compiler.script.compile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @Author: zhangchong
 * @Description:
 */
public class PlayScriptCompilerTest {

    private boolean verbose;
    private boolean astDump;

    @Before
    public void setUp() throws Exception {
        verbose = true;
        astDump = true;
    }

    @After
    public void tearDown() throws Exception {
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
        PlayScriptCompiler compiler = new PlayScriptCompiler();
        AnnotatedTree at = compiler.compile(script, verbose, astDump);

        Assert.assertFalse(at.hasCompilationError());
    }

}