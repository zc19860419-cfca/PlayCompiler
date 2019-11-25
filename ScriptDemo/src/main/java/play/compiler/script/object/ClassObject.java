package play.compiler.script.object;

import play.compiler.script.compile.ClassScope;

/**
 * @Author: zhangchong
 * @Description:
 */
public class ClassObject extends PlayObject {
    /**
     * 类的类型也就是类作用域
     */
    protected ClassScope type = null;

    /**
     * 父类的实例
     */
    protected PlayObject parentObject = null;
}
