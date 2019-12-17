package play.compiler.script.runtime;

import play.compiler.script.compile.Variable;
import play.compiler.script.object.PlayObject;

/**
 * @Author: zhangchong
 * @Description: 对栈中的值的引用
 */
public interface LValue {
    Object getValue();

    void setValue(Object value);

    Variable getVariable();

    PlayObject getValueContainer();
    //public StackFrame getFrame();
}
