package play.compiler.script.compile;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.script.runtime.FunctionType;
import play.compiler.script.runtime.Type;
import play.compiler.utils.Args;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: zhangchong
 * @Description: 作用域 目前划分了三种作用域，分别是块作用域（Block）、函数作用域（FunctionScope）和类作用域（ClassScopeAndType）
 */
public abstract class Scope extends Symbol {
    private static Logger LOG = LoggerFactory.getLogger(Scope.class);
    /**
     * 该Scope中的成员,包括变量 方法和类等
     */
    protected List<Symbol> symbols = new LinkedList<>();

    /**
     * 指定作用域中是否包含某个 Function.
     * 这是个静态方法,可以用作工具方法.避免类里要查找父类的情况
     *
     * @param scope
     * @param name
     * @param paramTypes 不允许为空,可以没有参数
     * @return
     */
    protected static FunctionScope getFunction(Scope scope, String name, List<Type> paramTypes) {
        Args.notNull(scope, "getFunction(3)#scope");
        Args.notEmpty(name, "getFunction(3)#name");
        Args.notNull(paramTypes, "getFunction(3)#paramTypes");
        FunctionScope result = null;
        FunctionScope thatFunction;
        for (Symbol s : scope.symbols) {
            if (s instanceof FunctionScope && s.name.equals(name)) {
                thatFunction = (FunctionScope) s;
                if (thatFunction.matchParameterTypes(paramTypes)) {
                    result = thatFunction;
                    break;
                }
            }

        }
        return result;

    }

    /**
     * 是否包含某个Function
     *
     * @param name
     * @param paramTypes 参数类型.(不允许为空)
     *                   如果没有参数,需要传入一个0长度的列表。
     * @return
     */
    protected FunctionScope getFunction(String name, List<Type> paramTypes) {
        Args.notEmpty(name, "getFunction(2)#name");
        Args.notNull(paramTypes, "getFunction(2)#paramTypes");
        return getFunction(this, name, paramTypes);
    }

    /**
     * 在指定作用域中获取一个能匹配相应的参数类型的函数类型的变量
     *
     * @param scope
     * @param name
     * @param paramTypes
     * @return
     */
    protected static Variable getFunctionVariable(Scope scope, String name, List<Type> paramTypes) {
        Variable result = null;
        Variable v;
        FunctionType functionType;
        for (Symbol s : scope.symbols) {
            if (s instanceof Variable && ((Variable) s).type instanceof FunctionType && s.name.equals(name)) {
                v = (Variable) s;
                functionType = (FunctionType) v.type;
                if (functionType.matchParameterTypes(paramTypes)) {
                    result = v;
                    break;
                }
            }
        }


        return result;
    }

    protected static Variable getVariable(Scope scope, String name) {
        Variable result = null;
        for (Symbol s : scope.symbols) {
            if (s instanceof Variable && s.name.equals(name)) {
                result = (Variable) s;
                break;
            }
        }
        return result;
    }

    /**
     * 向scope中添加符号，同时设置好该符号的enclosingScope
     *
     * @param symbol
     */
    protected void addSymbol(Symbol symbol) {
        Args.notNull(symbol, "symbol");
        symbols.add(symbol);
        symbol.enclosingScope = this;
    }

    /**
     * 查看所有作用域中是否包含某个Variable
     *
     * @param name
     * @return
     */
    protected Variable getVariable(String name) {
        return getVariable(this, name);
    }

    protected static ClassScopeAndType getClass(Scope scope, String name) {
        ClassScopeAndType result = null;
        for (Symbol s : scope.symbols) {
            if (s instanceof ClassScopeAndType && s.name.equals(name)) {
                result = (ClassScopeAndType) s;
                break;
            }
        }
        return result;
    }

    /**
     * 是否包含某个Class
     *
     * @param name
     * @return
     */
    protected ClassScopeAndType getClass(String name) {
        return getClass(this, name);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Scope ");
        builder.append(name);
        return builder.toString();
    }

    /**
     * 在当前作用域中获取一个能匹配相应的参数类型的函数类型的变量
     *
     * @param name       函数名
     * @param paramTypes 函数参数类型列表
     * @return
     */
    protected Variable getFunctionVariable(String name, List<Type> paramTypes) {
        return getFunctionVariable(this, name, paramTypes);
    }

    /**
     * 是否包含某个Symbol
     *
     * @param symbol
     * @return
     */
    protected boolean containsSymbol(Symbol symbol) {
        return symbols.contains(symbol);
    }
}
