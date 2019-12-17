package play.compiler.script.runtime;

import play.compiler.script.compile.Scope;

/**
 * @Author: zhangchong
 * @Description:
 */
public enum PrimitiveType implements Type {
    // 把常见的基础数据类型都定义出来
    Integer("Integer"),
    Long("Long"),
    Float("Float"),
    Double("Double"),
    Boolean("Boolean"),
    Byte("Byte"),
    Char("Char"),
    Short("Short"),
    String("String"),
    Null("Null");

    private String name;


//    public static PrimitiveType Long = new PrimitiveType("Long");
//    public static PrimitiveType Float = new PrimitiveType("Float");
//    public static PrimitiveType Double = new PrimitiveType("Double");
//    public static PrimitiveType Boolean = new PrimitiveType("Boolean");
//    public static PrimitiveType Byte = new PrimitiveType("Byte");
//    public static PrimitiveType Char = new PrimitiveType("Char");
//    public static PrimitiveType Short = new PrimitiveType("Short");
//    public static PrimitiveType String = new PrimitiveType("String");
//    public static PrimitiveType Null = new PrimitiveType("null");

    /**
     * 没有公共的构造方法
     *
     * @param name
     */
    PrimitiveType(String name) {
        this.name = name;
    }

    public static PrimitiveType getUpperType(Type type1, Type type2) {
        PrimitiveType type = null;
        if (type1 == String || type2 == String) {
            type = String;
        } else if (type1 == Double || type2 == Double) {
            type = Double;
        } else if (type1 == Float || type2 == Float) {
            type = Float;
        } else if (type1 == Long || type2 == Long) {
            type = Long;
        } else if (type1 == Integer || type2 == Integer) {
            type = Integer;
        } else if (type1 == Short || type2 == Short) {
            type = Short;
        } else {
            type = Byte;
        }
        // TODO 以上这些规则有没有漏洞？
        return type;
    }

    /**
     * 某个类型是不是数值型的(以便进行数值型运算)
     *
     * @param type
     * @return
     */
    public static boolean isNumeric(Type type) {
        return (type == PrimitiveType.Byte
                || type == PrimitiveType.Short
                || type == PrimitiveType.Integer
                || type == PrimitiveType.Double
                || type == PrimitiveType.Float
                || type == PrimitiveType.Long);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Scope getEnclosingScope() {
        return null;
    }

    @Override
    public boolean isType(Type type) {
        return this == type;
    }

    @Override
    public String toString() {
        return name;
    }
}
