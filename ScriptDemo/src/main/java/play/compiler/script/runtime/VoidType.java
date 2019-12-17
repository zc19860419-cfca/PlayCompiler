package play.compiler.script.runtime;

import play.compiler.script.compile.Scope;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description 函数返回类型 Void
 */
public final class VoidType implements Type {

    //只保留一个实例即可。
    private static VoidType voidType = new VoidType();

    private VoidType() {
    }

    public static VoidType instance() {
        return voidType;
    }

    @Override
    public String getName() {
        return "void";
    }

    @Override
    public Scope getEnclosingScope() {
        return null;
    }

    @Override
    public boolean isType(Type type) {
        return this == type;
    }

    @Override
    public String toString() {
        return "void";
    }

}