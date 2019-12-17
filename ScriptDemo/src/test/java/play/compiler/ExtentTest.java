package play.compiler;

import org.junit.Test;

/**
 * Unit test for simple PlayScript.
 */
public class ExtentTest {
    @Test
    public void TestExtent() {
        Extent2 extent2 = new Extent2();
        // 获得对象引用
        StringBuffer c = extent2.myMethod();
        System.out.println(c);
        // 修改内存中的内容
        c.append("World!");
        System.out.println("TestExtent#" + c);
        // 跟在 myMethod() 中打印的值相同
        System.out.println("TestExtent#myMethod#" + System.identityHashCode(c));
    }

    class Extent2 {
        StringBuffer myMethod() {
            // 在堆中生成对象实例
            StringBuffer b = new StringBuffer();
            b.append("Hello ");
            // 打印内存地址
            System.out.println("Extent2#myMethod#" + System.identityHashCode(b));
            // 返回对象引用，本质是一个内存地址
            return b;
        }
    }
}
