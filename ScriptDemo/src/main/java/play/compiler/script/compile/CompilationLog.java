package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description 记录编译过程中产生的信息
 */
public class CompilationLog {
    public static int INFO = 0;
    public static int WARNING = 1;
    public static int ERROR = 2;
    protected String message = null;
    protected int line;
    protected int positionInLine;
    /**
     * 相关的AST节点
     */
    protected ParserRuleContext ctx;
    /**
     * log的类型，包括信息、警告、错误。
     */
    protected int type = INFO;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(1024);
        builder.append(message).append(" @").append(line).append(":").append(positionInLine);
        return builder.toString();
    }
}
