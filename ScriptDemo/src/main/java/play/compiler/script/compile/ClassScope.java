package play.compiler.script.compile;

import play.compiler.script.generate.PlayScriptParser.ClassDeclarationContext;
import play.compiler.script.runtime.Type;

/**
 * @Author: zhangchong
 * @Description: 类作用域
 */
public class ClassScope extends Scope implements Type {

    /**
     * 最顶层的基类
     */
    private static ClassScope rootClass = new ClassScope("Object", null);
    /**
     * 父类
     */
    private Class parentClass = null;
    //这个类的This变量
    private This thisRef = null;
    private Super superRef = null;

    public ClassScope(String name, ClassDeclarationContext ctx) {
        this.name = name;
        this.ctx = ctx;

        thisRef = new This(this, ctx);
        thisRef.type = this;

    }

    @Override
    public boolean isType(Type type) {
        //TODO
        return false;
    }
}
