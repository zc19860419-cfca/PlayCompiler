package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * @Author: zhangchong
 * @Description: 用来表示"super"关键字的符号
 */
public class Super extends Variable {
    Super(ClassScopeAndType theClass, ParserRuleContext ctx) {
        super("super", theClass, ctx);
    }
}
