package play.compiler.script.compile;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import play.compiler.script.generate.PlayScriptLexer;
import play.compiler.script.generate.PlayScriptParser;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description
 */
public class PlayScriptCompiler implements Compiler {
    private static Logger LOG = LoggerFactory.getLogger(PlayScriptCompiler.class);

    private AnnotatedTree at = null;
    private PlayScriptLexer lexer = null;
    private PlayScriptParser parser = null;

    @Override
    public AnnotatedTree compile(String script, boolean verbose, boolean astDump) {
        at = new AnnotatedTree();

        //词法分析
        lexer = new PlayScriptLexer(CharStreams.fromString(script));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        //语法分析
        parser = new PlayScriptParser(tokens);
        at.ast = parser.prog();

        //语义分析
        final ParseTreeWalker walker = new ParseTreeWalker();

        //多步的语义分析
        //优点:1.代码更清晰 2.允许使用在声明之前,这在支持面向对象、递归函数等特征时是必须的。
        //pass1:类型和作用域解析（TypeAndScopeScanner.java）
        TypeAndScopeScanner pass1 = new TypeAndScopeScanner(at);
        walker.walk(pass1, at.ast);

        //pass2:把变量,类继承,函数声明的类型都解析出来.也就是所有声明时用到类型的地方.
        TypeResolver pass2 = new TypeResolver(at);
        walker.walk(pass2, at.ast);

        //pass3:解析变量应用,函数引用.另外还做了类型推断
        RefResolver pass3 = new RefResolver(at);
        walker.walk(pass3, at.ast);

        //pass4:类型检查:比如当赋值语句左右两边的类型不兼容的时候,就可以报错
        TypeChecker pass4 = new TypeChecker(at);
        walker.walk(pass4, at.ast);
        //pass5:其他语义检查
        SematicChecker pass5 = new SematicChecker(at);
        walker.walk(pass5, at.ast);

        //pass6:

        //打印AST
        if (verbose || astDump) {
            dumpAST();
        }

        //打印符号表
        if (verbose) {
            dumpSymbols();
        }

        return at;
    }

    /**
     * 打印符号表
     */
    @Override
    public void dumpSymbols() {
        if (at != null) {
            LOG.info("dumpSymbols:\n{}", at.getScopeTreeString());
        }
    }

    /**
     * 打印AST，以lisp格式
     */
    @Override
    public void dumpAST() {
        if (at != null) {
            LOG.info("dumpAST:\n{}", at.ast.toStringTree(parser));
        }
    }

    /**
     * 输出编译信息
     */
    @Override
    public void dumpCompilationLogs() {
        if (at != null) {
            for (CompilationLog log : at.logs) {
                LOG.info(log.toString());
            }
        }
    }

    @Override
    public Object Execute(AnnotatedTree at) {
        ASTEvaluator visitor = new ASTEvaluator(at);
        Object result = visitor.visit(at.ast);
        return result;
    }
}
