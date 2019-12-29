package play.compiler.script.object;

/**
 * @Author: zhangchong
 * @Description: 代表Return语句的返回值
 */
public class ReturnObject {
    /**
     * 真正的返回值
     */
    Object returnValue = null;

    public ReturnObject(Object value) {
        this.returnValue = value;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * 在打印时输出ReturnObject
     *
     * @return
     */
    @Override
    public String toString() {
        return "ReturnObject";
    }

}
