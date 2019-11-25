package play.compiler.script.compile;


import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import play.compiler.script.runtime.Type;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhangchong
 * @Description: 注释树。
 * 语义分析的结果都放在这里。跟AST的节点建立关联。包括：
 * 1.类型信息，包括基本类型和用户自定义类型；
 * 2.变量和函数调用的消解；
 * 3.作用域Scope。在Scope中包含了该作用域的所有符号。Variable、Function、Class等都是符号。
 */
public class AnnotatedTree {
    private static Logger LOG = LoggerFactory.getLogger(AnnotatedTree.class);
    /**
     * AST
     */
    protected ParseTree ast = null;

    /**
     * 解析出来的所有类型，包括类和函数，以后还可以包括数组和枚举。类的方法也作为单独的要素放进去。
     */
    protected List<Type> types = new LinkedList<Type>();

    /**
     * AST节点对应的Symbol
     */
    protected Map<ParserRuleContext, Symbol> symbolOfNode = new HashMap<ParserRuleContext, Symbol>();

    /**
     * AST节点对应的Scope，如for、函数调用会启动新的Scope
     */
    protected Map<ParserRuleContext, Scope> node2Scope = new HashMap<ParserRuleContext, Scope>();

    /**
     * 用于做类型推断，每个节点推断出来的类型
     */
    protected Map<ParserRuleContext, Type> typeOfNode = new HashMap<ParserRuleContext, Type>();
    /**
     * 语义分析过程中生成的信息，包括普通信息、警告和错误
     */
    protected List<CompilationLog> logs = new LinkedList<CompilationLog>();
    /**
     * 全局命名空间
     */
    NameSpace nameSpace = null;


    protected AnnotatedTree() {

    }

    public boolean hasCompilationError() {
        return false;
    }

    public void setNameSpace(NameSpace scope) {
        this.nameSpace = scope;
    }


    /**
     * 记录编译错误和警告
     *
     * @param message
     * @param type    信息类型，ComplilationLog中的INFO、WARNING和ERROR
     * @param ctx
     */
    protected void log(String message, int type, ParserRuleContext ctx) {
        CompilationLog log = new CompilationLog();
        log.ctx = ctx;
        log.message = message;
        log.line = ctx.getStart().getLine();
        log.positionInLine = ctx.getStart().getStartIndex();
        log.type = type;

        logs.add(log);
        LOG.info(log.toString());
    }

    public void log(String message, ParserRuleContext ctx) {
        this.log(message, CompilationLog.ERROR, ctx);
    }

    /**
     * 通过名称查找Class。逐级Scope向外层查找。
     *
     * @param scope
     * @param idName
     * @return
     */
    protected ClassScope lookupClass(Scope scope, String idName) {
        ClassScope rtn = scope.getClass(idName);

        if (rtn == null && scope.enclosingScope != null) {
            rtn = lookupClass(scope.enclosingScope, idName);
        }
        return rtn;
    }

    /**
     * 输出本Scope中的内容，包括每个变量的名称、类型。
     *
     * @return 树状显示的字符串
     */
    public String getScopeTreeString() {
        StringBuffer sb = new StringBuffer();
        scopeToString(sb, nameSpace, "");
        return sb.toString();
    }

    /**
     * 打印作用域中的所有嵌套作用域名字
     *
     * @param sb
     * @param scope
     * @param indent
     */
    private void scopeToString(StringBuffer sb, Scope scope, String indent) {
        sb.append(indent).append(scope).append('\n');
        for (Symbol symbol : scope.symbols) {
            if (symbol instanceof Scope) {
                scopeToString(sb, (Scope) symbol, indent + '\t');
            } else {
                sb.append(indent).append('\t').append(symbol).append('\n');
            }
        }
    }

    /**
     * 获取某个节点所在的scope
     * 算法:逐级查找父节点,找到一个对应着Scope的上级节点
     *
     * @param node
     * @return
     */
    public Scope enclosingScopeOfNode(ParserRuleContext node) {
        Scope result = null;
        ParserRuleContext parent = node.getParent();
        if (null != parent) {
            final Scope rtn = node2Scope.get(parent);
            if (null == rtn) {
                result = enclosingScopeOfNode(parent);
            }
        }
        return result;
    }
}
