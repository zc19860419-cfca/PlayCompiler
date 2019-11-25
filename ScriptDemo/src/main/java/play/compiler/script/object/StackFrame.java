package play.compiler.script.object;

import play.compiler.script.compile.BlockScope;
import play.compiler.script.compile.Scope;
import play.compiler.script.compile.Variable;

/**
 * @Author: zhangchong
 * @Description: 栈帧对象:保存当前作用域的所有本地变量的值,当退出这个作用域的时候,这个栈帧就被弹出,里面的变量就失效了
 */
public class StackFrame {
    /**
     * 该 frame 所对应的 scope
     */
    Scope scope = null;

    /**
     * 放parent scope所对应的frame的指针，就叫parentFrame吧，便于提高查找效率。
     * 要访问上一级Scope中的变量数据,需要顺着栈帧的 parentFrame 找。
     * 规则：
     * 1.如果是同一级函数调用，跟上一级的parentFrame相同；
     * 2.如果是下一级的函数调用或For、If等block，parentFrame是自己；
     * 3.如果是一个闭包（如何判断？），那么要带一个存放在堆里的环境。
     */
    StackFrame parentFrame = null;

    /**
     * 实际存放变量的地方
     */
    PlayObject object = null;

    /**
     * 为块作用域创建一个 StackFrame
     *
     * @param scope
     */
    public StackFrame(BlockScope scope) {
        this.scope = scope;
        this.object = new PlayObject();
    }

    /**
     * 为类对象创建一个 StackFrame
     *
     * @param object
     */
    public StackFrame(ClassObject object) {
        this.scope = object.type;
        this.object = new PlayObject();
    }

    /**
     * 为函数调用创建一个 StackFrame
     *
     * @param object
     */
    public StackFrame(FunctionObject object) {
        this.scope = object.functionScope;
        this.object = object;
    }

    /**
     * 判断本地栈中有没有包含某个变量的数据
     *
     * @param variable
     * @return
     */
    protected boolean contains(Variable variable) {
        boolean result = false;
        if (null != object && null != object.fields) {
            result = object.fields.containsKey(variable);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(scope);
        if (parentFrame != null) {
            builder.append(" -> ").append(parentFrame);
        }
        return builder.toString();
    }

}
