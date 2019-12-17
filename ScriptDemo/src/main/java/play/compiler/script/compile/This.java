package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * @Author: zhangchong
 * @Description: 用来表示"this"关键字的符号
 */
public class This extends Variable {
    This(ClassScopeAndType theClassScopeAndType, ParserRuleContext ctx) {
        super("this", theClassScopeAndType, ctx);
    }
}
