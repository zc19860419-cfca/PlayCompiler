package play.compiler.script.utils;

import play.compiler.script.runtime.PrimitiveType;
import play.compiler.script.runtime.Type;
import play.compiler.utils.Args;

/**
 * @Author: zhangchong
 * @Description: 四则运算
 */
public class NumberUtils {
    public static Object div(Object leftObject, Object rightObject, Type targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() / ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() / ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() / ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() / ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() / ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("div#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("div#invalid op=>type not PrimitiveType:" + targetType);
        }
        return rtn;
    }

    public static Object mul(Object leftObject, Object rightObject, Type targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() * ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() * ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() * ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() * ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() * ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("mul#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("mul#invalid op=>type not PrimitiveType:" + targetType);
        }
        return rtn;
    }

    public static Object minus(Object leftObject, Object rightObject, Type targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() - ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() - ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() - ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() - ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() - ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("minus#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("minus#invalid op=>type not PrimitiveType:" + targetType);
        }
        return rtn;
    }

    public static Object add(Object leftObject, Object rightObject, Type targetType) {
        Args.notNull(leftObject, "add#leftObject");
        Args.notNull(rightObject, "add#rightObject");
        Args.notNull(targetType, "add#targetType");
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() + ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() + ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() + ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() + ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() + ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("add#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("add#invalid op=>type not PrimitiveType:" + targetType);
        }
        return rtn;
    }
}
