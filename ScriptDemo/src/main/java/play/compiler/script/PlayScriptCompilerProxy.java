package play.compiler.script;

import play.compiler.script.compile.AnnotatedTree;
import play.compiler.script.compile.Compiler;
import play.compiler.script.compile.PlayScriptCompiler;

/**
 * @Author: zhangchong
 * @Description: 对外的调用类
 */
public final class PlayScriptCompilerProxy implements Compiler {

    private final PlayScriptCompiler compiler;

    public PlayScriptCompilerProxy(PlayScriptCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public AnnotatedTree compile(String script, boolean verbose, boolean astDump) {
        return compiler.compile(script, verbose, astDump);
    }

    @Override
    public void dumpSymbols() {
        compiler.dumpSymbols();
    }

    /**
     * 打印AST，以lisp格式
     */
    @Override
    public void dumpAST() {
        compiler.dumpAST();
    }

    /**
     * 输出编译信息
     */
    @Override
    public void dumpCompilationLogs() {
        compiler.dumpCompilationLogs();
    }

    /**
     * @param at
     * @return
     */
    @Override
    public Object Execute(AnnotatedTree at) {
        return compiler.Execute(at);
    }
}
