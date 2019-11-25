package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * @Author: zhangchong
 * @Description: 块作用域
 */
public class BlockScope extends Scope {
    /**
     * block 索引
     */
    private static int index = 1;

    protected BlockScope() {
        this.name = "block" + index++;
    }

    protected BlockScope(Scope enclosingScope, ParserRuleContext ctx) {
        this.name = "block" + index++;
        this.enclosingScope = enclosingScope;
        this.ctx = ctx;
    }

    @Override
    public String toString() {
        return "Block " + name;
    }
}
