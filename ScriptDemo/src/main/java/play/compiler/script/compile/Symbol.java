package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Objects;

/**
 * @Author: zhangchong
 * @Description: 用于表示面向对象的作用域
 */
public abstract class Symbol {
    /**
     * 符号的名称
     */
    protected String name = null;

    /**
     * 所属的作用域
     */
    protected Scope enclosingScope = null;

    /**
     * 可见性,比如 public 还是 private
     */
    protected int visibility = 0;

    /**
     * Symbol 关联的 AST 节点
     */
    protected ParserRuleContext ctx = null;

    public String getName() {
        return name;
    }

    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    protected ParserRuleContext getParserRuleContext() {
        return ctx;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (this == o) {
            result = true;
        } else if (o == null || getClass() != o.getClass()) {
            result = false;
        } else {
            Symbol symbol = (Symbol) o;
            result = visibility == symbol.visibility &&
                    Objects.equals(name, symbol.name) &&
                    Objects.equals(enclosingScope, symbol.enclosingScope);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enclosingScope, visibility);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Symbol ");
        builder.append(name);
        builder.append(" -> ");
        builder.append(enclosingScope);
        return builder.toString();
    }
}
