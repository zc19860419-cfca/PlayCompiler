package play.compiler.script.object;

import play.compiler.script.compile.Variable;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhangchong
 * @Description: PlayScript的对象
 */
public class PlayObject {
    /**
     * 成员变量
     */
    protected Map<Variable, Object> fields = new HashMap<Variable, Object>();

    public Object getValue(Variable variable) {
        Object rtn = fields.get(variable);
        //TODO 父类的属性如何返回？还是说都在这里了？

        /**
         * 替换成自己的NullObject
         */
        if (rtn == null) {
            rtn = NullObject.instance();
        }
        return rtn;
    }

    public void setValue(Variable variable, Object value) {
        fields.put(variable, value);
    }

    public Map<Variable, Object> getFields() {
        return fields;
    }
}
