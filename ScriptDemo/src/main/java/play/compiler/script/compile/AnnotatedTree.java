package play.compiler.script.compile;


import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import play.compiler.script.generate.PlayScriptParser.ClassDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.FunctionDeclarationContext;
import play.compiler.script.runtime.Type;
import play.compiler.utils.Args;

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
     * 用于做类型推断，记录每个节点推断出来的类型
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
     * 获取某个节点所在的作用域
     * 算法:逐级查找父节点,找到一个对应着Scope的上级节点
     *
     * @param node
     * @return
     */
    public Scope enclosingScopeOfNode(ParserRuleContext node) {
        Args.notNull(node, "enclosingScopeOfNode#node");
        Scope result = null;
        ParserRuleContext parent = node.getParent();
        if (null != parent) {
            result = node2Scope.get(parent);
            if (null == result) {
                result = enclosingScopeOfNode(parent);
            }
        }
        return result;
    }

    /**
     * 获取某个节点所属的class
     * <p>
     * classDeclaration
     * : CLASS IDENTIFIER
     * (EXTENDS typeType)?
     * (IMPLEMENTS typeList)?
     * classBody
     * ;
     *
     * @param ctx
     * @return
     */
    public ClassScopeAndType enclosingClassOfNode(RuleContext ctx) {
        Args.notNull(ctx, "enclosingClassOfNode#node");
        ClassScopeAndType result;
        if (ctx.parent instanceof ClassDeclarationContext) {
            result = (ClassScopeAndType) node2Scope.get(ctx.parent);
        } else if (null == ctx.parent) {
            result = null;
        } else {
            result = enclosingClassOfNode(ctx.parent);
        }
        return result;
    }

    /**
     * 获取包含某节点的函数
     *
     * @param ctx
     * @return
     */
    public FunctionScope enclosingFunctionOfNode(RuleContext ctx) {
        FunctionScope result;
        if (ctx.parent instanceof FunctionDeclarationContext) {
            result = (FunctionScope) node2Scope.get(ctx.parent);
        } else if (ctx.parent == null) {
            result = null;
        } else {
            result = enclosingFunctionOfNode(ctx.parent);
        }
        return result;
    }

    /**
     * 通过符号名称查找父类类型
     *
     * @param idName
     * @return
     */
    public Type lookupType(String idName) {
        Args.notEmpty(idName, "lookupType#idName");
        Type result = null;
        for (Type type : types) {
            if (type.getName().equals(idName)) {
                result = type;
                break;
            }
        }
        return result;
    }

    /**
     * 通过名称查找Class。逐级Scope向外层查找。
     *
     * @param currentScope
     * @param idName
     * @return
     */
    protected ClassScopeAndType lookupClass(Scope currentScope, String idName) {
        Args.notNull(currentScope, "lookupClass#currentScope");
        Args.notEmpty(idName, "lookupClass#idName");
        ClassScopeAndType rtn = currentScope.getClass(idName);

        if (rtn == null && currentScope.enclosingScope != null) {
            rtn = lookupClass(currentScope.enclosingScope, idName);
        }
        return rtn;
    }

    /**
     * 通过名称查找Variable
     * 从当前Scope 逐级向上级 Scope 查找。
     *
     * @param currentScope
     * @param idName
     * @return
     */
    protected Variable lookupVariable(Scope currentScope, String idName) {
        Args.notNull(currentScope, "lookupVariable#currentScope");
        Args.notEmpty(idName, "lookupVariable#idName");
        Variable result = currentScope.getVariable(idName);

        if (result == null && currentScope.enclosingScope != null) {
            result = lookupVariable(currentScope.enclosingScope, idName);
        }
        return result;
    }

    /**
     * 逐级查找函数或者方法.仅通过名字来查找,如果有重名的,返回第一个
     * FIXME:遇到重名的要报警
     *
     * @param currentScope
     * @param name
     * @return
     */
    public FunctionScope lookupFunction(Scope currentScope, String name) {
        Args.notNull(currentScope, "lookupFunction#currentScope");
        Args.notEmpty(name, "lookupFunction#name");
        FunctionScope result = null;
        if (currentScope instanceof ClassScopeAndType) {
            result = getMethodOnlyByName((ClassScopeAndType) currentScope, name);
        }
        return result;
    }

    /**
     * 通过方法的名称和方法签名查找Function
     * 逐级向上Scope查找。
     *
     * @param currentScope
     * @param idName
     * @param paramTypes
     * @return
     */
    protected FunctionScope lookupFunction(Scope currentScope, String idName, List<Type> paramTypes) {
        Args.notNull(currentScope, "lookupFunction#currentScope");
        Args.notEmpty(idName, "lookupFunction#idName");
        FunctionScope result = currentScope.getFunction(idName, paramTypes);

        if (result == null && currentScope.enclosingScope != null) {
            result = lookupFunction(currentScope.enclosingScope, idName, paramTypes);
        }
        return result;
    }

    /**
     * 查找函数型变量,逐级向上查找.
     *
     * @param scope
     * @param idName
     * @param paramTypes
     * @return
     */
    protected Variable lookupFunctionVariable(Scope scope, String idName, List<Type> paramTypes) {
        Variable result = scope.getFunctionVariable(idName, paramTypes);

        if (null == result && null != scope.enclosingScope) {
            result = lookupFunctionVariable(scope.enclosingScope, idName, paramTypes);
        }
        return result;
    }

    /**
     * 对于类而言,如果当前类没有找到,就需要逐级向上对他的父类进行搜索
     *
     * @param theClass 类类型和作用域
     * @param name     函数名/方法名
     * @return 拥有方法作用域的方法对象
     */
    private FunctionScope getMethodOnlyByName(ClassScopeAndType theClass, String name) {
        FunctionScope result = getFunctionOnlyByName(theClass, name);

        if (result == null && theClass.getParentClass() != null) {
            result = getMethodOnlyByName(theClass.getParentClass(), name);
        }

        return result;
    }

    /**
     * 在指定作用域中查找指定方法名的函数/方法
     *
     * @param scope
     * @param name
     * @return
     */
    private FunctionScope getFunctionOnlyByName(Scope scope, String name) {
        FunctionScope result = null;
        for (Symbol symbol : scope.symbols) {
            if (symbol instanceof FunctionScope && symbol.name.equals(name)) {
                result = (FunctionScope) symbol;
                break;
            }
        }
        return result;
    }


}
