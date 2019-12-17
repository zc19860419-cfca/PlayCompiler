package play.compiler.script.object;

import play.compiler.script.compile.ClassScopeAndType;

/**
 * @Author: zhangchong
 * @Description: 类对象
 */
public class ClassObject extends PlayObject {
    /**
     * 类的类型也就是类作用域
     */
    protected ClassScopeAndType type = null;

    /**
     * 父类的实例
     */
    protected PlayObject parentObject = null;

    public ClassScopeAndType getType() {
        return type;
    }

    public void setType(ClassScopeAndType type) {
        this.type = type;
    }

    public PlayObject getParentObject() {
        return parentObject;
    }

    public void setParentObject(PlayObject parentObject) {
        this.parentObject = parentObject;
    }
}
