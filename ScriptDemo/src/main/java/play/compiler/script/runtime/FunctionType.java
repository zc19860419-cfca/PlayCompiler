package play.compiler.script.runtime;

import java.util.List;

/**
 * @Author: zhangchong
 * @Description: 函数类型
 */
public interface FunctionType extends Type {
    Type getReturnType();

    List<Type> getParamTypes();

    boolean matchParameterTypes(List<Type> paramTypes);
}
