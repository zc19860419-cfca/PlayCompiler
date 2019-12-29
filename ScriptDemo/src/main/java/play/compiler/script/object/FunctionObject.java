package play.compiler.script.object;

import play.compiler.script.compile.FunctionScope;
import play.compiler.script.compile.Variable;

/**
 * @Author: zhangchong
 * @Description: 存放一个函数运行时的本地变量的值，包括参数的值。
 */
public class FunctionObject extends PlayObject {
    /**
     * 类型
     */
    protected FunctionScope functionScope = null;

    /**
     * 接收者所在的scope。缺省是function的enclosingScope，也就是词法的Scope。
     * 当赋值给一个函数型变量的时候，要修改receiverEnclosingScope等于这个变量的enclosingScope。
     */
    protected Variable receiver = null;

    public FunctionObject(FunctionScope functionScope) {
        this.functionScope = functionScope;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public FunctionScope getFunctionScope() {
        return functionScope;
    }

    public void setFunctionScope(FunctionScope functionScope) {
        this.functionScope = functionScope;
    }
}
