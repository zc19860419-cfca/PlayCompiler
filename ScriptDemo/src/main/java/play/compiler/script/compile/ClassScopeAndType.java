package play.compiler.script.compile;

import play.compiler.script.generate.PlayScriptParser.ClassDeclarationContext;
import play.compiler.script.runtime.Type;
import play.compiler.utils.Args;

import java.util.List;

/**
 * @Author: zhangchong
 * @Description: 类作用域
 */
public class ClassScopeAndType extends Scope implements Type {

    /**
     * 最顶层的基类
     */
    private static ClassScopeAndType rootClass = new ClassScopeAndType("Object", null);
    /**
     * 父类
     */
    private ClassScopeAndType parentClass = null;
    //这个类的This变量
    private This thisRef = null;
    private Super superRef = null;

    public ClassScopeAndType(String name, ClassDeclarationContext ctx) {
        this.name = name;
        this.ctx = ctx;

        thisRef = new This(this, ctx);
        thisRef.type = this;

    }

    /**
     * 当自身是目标类型的子类型的时候，返回true;
     *
     * @param type 目标类型
     * @return
     */
    @Override
    public boolean isType(Type type) {
        boolean result = false;
        if (type == this) {
            result = true;
        } else {
            if (type instanceof ClassScopeAndType) {
                result = ((ClassScopeAndType) type).isAncestor(this);
            }
        }
        return result;
    }

    public This getThis() {
        return thisRef;
    }

    public Super getSuper() {
        return superRef;
    }

    /**
     * 本类型是不是另一个类型的祖先类型
     *
     * @param theClass
     * @return
     */
    private boolean isAncestor(ClassScopeAndType theClass) {
        boolean result = false;
        if (theClass.getParentClass() != null) {
            if (theClass.getParentClass() == this) {
                result = true;
            } else {
                result = isAncestor(theClass.getParentClass());
            }
        }
        return result;
    }

    protected ClassScopeAndType getParentClass() {
        return parentClass;
    }

    protected void setParentClass(ClassScopeAndType theClass) {
        parentClass = theClass;

        //其实superRef引用的也是自己
        superRef = new Super(parentClass, ctx);
        superRef.type = parentClass;
    }

    /**
     * 在自身及父类中找到某个方法
     *
     * @param name
     * @param paramTypes 参数类型列表,该参数不允许为空.如果没有参数.需要传入一个0长度的列表.
     * @return
     */
    protected FunctionScope getFunction(String name, List<Type> paramTypes) {
        Args.notNull(paramTypes, "ClassScopeAndType#getFunction#paramTypes");
        //TODO 是否要检查visibility?
        FunctionScope rtn = super.getFunction(name, paramTypes);

        if (rtn == null && parentClass != null) {
            rtn = parentClass.getFunction(name, paramTypes);
        }

        return rtn;
    }

    /**
     * 首先在当前作用域中查找一个能匹配相应的参数类型的方法类型的变量
     * 然后再向上去查找父类中有没有能匹配相应的参数类型的方法类型的变量
     *
     * @param name       函数名
     * @param paramTypes 函数参数类型列表
     * @return
     */
    protected Variable getFunctionVariable(String name, List<Type> paramTypes) {
        //TODO 是否要检查visibility?
        Variable result = super.getFunctionVariable(name, paramTypes);

        if (null == result && null != parentClass) {
            result = parentClass.getFunctionVariable(name, paramTypes);
        }

        return result;
    }

    /**
     * 找到某个构建函数,不需要往父类去找,在本级找就行了
     * 就是用类的名字来查找有无该类方法
     *
     * @param paramTypes
     * @return
     */
    protected FunctionScope findConstructor(List<Type> paramTypes) {
        //TODO 是否要检查visibility?
        return super.getFunction(name, paramTypes);
    }

    @Override
    public String toString() {
        return "Class " + name;
    }
}
