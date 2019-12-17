package play.compiler.script.compile;

/**
 * @Author: zhangchong
 * @Description:
 */
public interface Compiler {

    /**
     * 执行脚本编译流程,执行六遍扫描
     *
     * @param script
     * @param verbose
     * @param astDump
     * @return
     */
    AnnotatedTree compile(String script, boolean verbose, boolean astDump);

    void dumpSymbols();

    /**
     * 打印AST，以lisp格式
     */
    void dumpAST();

    /**
     * 输出编译信息
     */
    void dumpCompilationLogs();

    /**
     * 获取编译后的执行结果
     *
     * @param at
     * @return
     */
    Object Execute(AnnotatedTree at);
}
