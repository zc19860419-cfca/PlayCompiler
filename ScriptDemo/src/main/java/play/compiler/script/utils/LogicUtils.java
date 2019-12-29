package play.compiler.script.utils;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.script.runtime.PrimitiveType;

/**
 * @Author: zhangchong
 * @Description: 逻辑运算
 */
public class LogicUtils {
    private static Logger LOG = LoggerFactory.getLogger(LogicUtils.class);

    public static Object GT(Object leftObject, Object rightObject, Object targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() > ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() > ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() > ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() > ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() > ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("GT#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("GT#invalid op=>type not PrimitiveType:" + targetType);
        }

        return rtn;
    }

    public static Object GE(Object leftObject, Object rightObject, Object targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() >= ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() >= ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() >= ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() >= ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() >= ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("GE#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("GE#invalid op=>type not PrimitiveType:" + targetType);
        }

        return rtn;
    }

    public static Object LT(Object leftObject, Object rightObject, Object targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() < ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() < ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() < ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() < ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() < ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("LT#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("LE#invalid op=>type not PrimitiveType:" + targetType);
        }

        return rtn;
    }

    public static Object LE(Object leftObject, Object rightObject, Object targetType) {
        Object rtn;
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() <= ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() <= ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() <= ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() <= ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() <= ((Number) rightObject).shortValue();
                    break;
                default:
                    throw new IllegalArgumentException("LE#invalid op=>Unknown type:" + targetType);
            }
        } else {
            throw new IllegalArgumentException("LE#invalid op=>type not PrimitiveType:" + targetType);
        }

        return rtn;
    }

    public static boolean EQ(Object leftObject, Object rightObject, Object targetType) {
        Boolean rtn;
        LOG.debug("EQ#Running...");
        LOG.debug("EQ#type={}", targetType);
        if (targetType instanceof PrimitiveType) {
            switch ((PrimitiveType) targetType) {
                case Integer:
                    rtn = ((Number) leftObject).intValue() == ((Number) rightObject).intValue();
                    break;
                case Float:
                    rtn = ((Number) leftObject).floatValue() == ((Number) rightObject).floatValue();
                    break;
                case Long:
                    rtn = ((Number) leftObject).longValue() == ((Number) rightObject).longValue();
                    break;
                case Double:
                    rtn = ((Number) leftObject).doubleValue() == ((Number) rightObject).doubleValue();
                    break;
                case Short:
                    rtn = ((Number) leftObject).shortValue() == ((Number) rightObject).shortValue();
                    break;
                default:
                    //对于对象实例、函数，直接比较对象引用
                    rtn = (leftObject == rightObject);
                    break;
            }
        } else {
            //对于对象实例、函数，直接比较对象引用
            rtn = (leftObject == rightObject);
        }
        LOG.debug("EQ#Finished");
        return rtn;
    }
}
