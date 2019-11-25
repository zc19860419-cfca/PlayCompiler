package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;
import play.compiler.script.runtime.Type;

/**
 * @Author: zhangchong
 * @Description:变量
 */
public class Variable extends Symbol {
    /**
     * 变量类型
     */
    protected Type type = null;

    /**
     * 作为parameter的变量的属性 缺省值
     */
    protected Object defaultValue = null;

    /**
     * 是否允许多次重复，这是一个创新的参数机制
     */
    protected Integer multiplicity = 1;

    /**
     *
     * @param name 变量名
     * @param enclosingScope 所属的作用域
     * @param ctx 符号关联的 AST 节点
     */
    protected Variable(String name, Scope enclosingScope, ParserRuleContext ctx) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        this.ctx = ctx;
    }

    /**
     * 是不是类的属性
     *
     * @return
     */
    public boolean isClassMember() {
        return enclosingScope instanceof ClassScope;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Variable ");
        builder.append(name);
        builder.append(" -> ");
        builder.append(type);
        return builder.toString();
    }
}
