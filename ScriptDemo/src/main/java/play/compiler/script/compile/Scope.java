package play.compiler.script.compile;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.utils.Args;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: zhangchong
 * @Description: 作用域 目前划分了三种作用域，分别是块作用域（Block）、函数作用域（FunctionScope）和类作用域（ClassScope）
 */
public abstract class Scope extends Symbol {
    private static Logger LOG = LoggerFactory.getLogger(Scope.class);
    /**
     * 该Scope中的成员,包括变量 方法和类等
     */
    protected List<Symbol> symbols = new LinkedList<>();

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

    protected boolean addSymbol(Symbol symbol) {
        Args.notNull(symbol, "symbol");
        boolean result;
        if (symbols.contains(symbol)) {
            LOG.error("addSymbol#symbol {} already in symbols", symbol);
            result = false;
        } else {
            symbols.add(symbol);
            result = true;
        }
        return result;
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

    protected static ClassScope getClass(Scope scope, String name) {
        ClassScope result = null;
        for (Symbol s : scope.symbols) {
            if (s instanceof ClassScope && s.name.equals(name)) {
                result = (ClassScope) s;
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
    protected ClassScope getClass(String name) {
        return getClass(this, name);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Scope ");
        builder.append(name);
        return builder.toString();
    }

}
