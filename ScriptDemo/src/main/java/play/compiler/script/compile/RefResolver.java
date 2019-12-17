package play.compiler.script.compile;

import play.compiler.script.generate.PlayScriptBaseListener;
import play.compiler.script.generate.PlayScriptParser;
import play.compiler.script.generate.PlayScriptParser.ExpressionContext;
import play.compiler.script.generate.PlayScriptParser.ExpressionListContext;
import play.compiler.script.generate.PlayScriptParser.FunctionCallContext;
import play.compiler.script.generate.PlayScriptParser.LiteralContext;
import play.compiler.script.generate.PlayScriptParser.PrimaryContext;
import play.compiler.script.generate.PlayScriptParser.ProgContext;
import play.compiler.script.generate.PlayScriptParser.VariableInitializerContext;
import play.compiler.script.runtime.FunctionType;
import play.compiler.script.runtime.PrimitiveType;
import play.compiler.script.runtime.Type;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: zhangchong
 * @Description: 语义解析的第三步:解析引用以及类型推断
 * 1.resolve 所有的本地变量引用,函数调用以及类成员引用
 * 2.类型推断:从下而上推断表达式的类型(S 属性).
 * 这两件事要放在一起做,因为:
 * (1) 对于变量,只有作了 resolve 才能推断出类型
 * (2) 对于 FunctionCall,只有把参数(表达式)的类型都推断出来,才能匹配到正确的函数(方法).
 * (3) 表达式里包含 FunctionCall,只有推断出表达式的类型(知道了是哪个 Function) 才能知道返回值
 * func = f(a,b,v)
 */
public class RefResolver extends PlayScriptBaseListener {
    private AnnotatedTree at = null;

    /**
     * this和super构造函数留到最后去 resolve, 因为它可能引用别的构造函数,必须等这些构造函数都 resolve 完
     */
    private List<FunctionCallContext> thisConstructorList = new LinkedList<>();
    private List<FunctionCallContext> superConstructorList = new LinkedList<>();

    public RefResolver(AnnotatedTree at) {
        this.at = at;
    }

    /**
     * 从下而上推断表达式的类型(S 属性)
     * primary
     * : '(' expression ')'
     * | THIS
     * | SUPER
     * | literal
     * | IDENTIFIER
     * ;
     *
     * @param ctx
     */
    @Override
    public void exitPrimary(PrimaryContext ctx) {
        //获取当前AST节点所属的作用域
        Scope scope = at.enclosingScopeOfNode(ctx);

        Type type = null;
        if (null != ctx.IDENTIFIER()) {
            //标识符
            final String idName = ctx.IDENTIFIER().getText();
            //找到标识符对应的 Variable
            Variable variable = at.lookupVariable(scope, idName);
            if (null == variable) {
                // 看看是不是函数,因为函数可以作为值来传递.这个时候,函数重名没法区分.
                // 因为普通 Scope 中的函数是不可以重名的,所以这应该是没有问题的.
                // TODO 但如果允许重名，那就不行了.
                // TODO 注意,查找 Function 的时候,可能会把类的方法包含进去.
                FunctionScope function = at.lookupFunction(scope, idName);
                if (function != null) {
                    at.symbolOfNode.put(ctx, function);
                    type = function;
                } else {
                    at.log("unknown variable or function: " + idName, ctx);
                }

            } else {
                at.symbolOfNode.put(ctx, variable);

                type = variable.type;
            }
        } else if (null != ctx.literal()) {
            //字面值
            type = at.typeOfNode.get(ctx.literal());
        } else if (null != ctx.expression()) {
            //括号表达式
            type = at.typeOfNode.get(ctx.expression());
        } else if (null != ctx.THIS()) {
            //this 关键字
            //找到Class类型的上级Scope
            ClassScopeAndType theClass = at.enclosingClassOfNode(ctx);
            if (null != theClass) {
                This variable = theClass.getThis();
                at.symbolOfNode.put(ctx, variable);
                type = theClass;
            } else {
                at.log("keyword \"this\" can only be used inside a class", ctx);
            }
        } else if (ctx.SUPER() != null) {
            //super关键字。看上去跟This关键字的用法完全一样？
            //找到Class类型的上级Scope
            ClassScopeAndType theClass = at.enclosingClassOfNode(ctx);
            if (theClass != null) {
                Super variable = theClass.getSuper();
                at.symbolOfNode.put(ctx, variable);

                type = theClass;
            } else {
                at.log("keyword \"super\" can only be used inside a class", ctx);
            }
        }

        //类型推断、冒泡
        at.typeOfNode.put(ctx, type);
    }

    /**
     * 离开 AST FunctionCall(函数调用) 节点时的回调
     * functionCall
     * : IDENTIFIER '(' expressionList? ')'
     * | THIS '(' expressionList? ')'
     * | SUPER '(' expressionList? ')'
     * ;
     *
     * @param ctx
     */
    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {

        if (ctx.THIS() != null) {
            // this
            thisConstructorList.add(ctx);
        } else if (ctx.SUPER() != null) {
            // super
            superConstructorList.add(ctx);
        } else if (ctx.IDENTIFIER().getText().equals("println")) {
            //TODO 临时代码，支持println
            // do nothing
        } else {
            handleOtherFunctionCall(ctx);
        }
    }

    /**
     * functionCall
     * : IDENTIFIER '(' expressionList? ')'
     * | THIS '(' expressionList? ')'
     * | SUPER '(' expressionList? ')'
     * ;
     * expressionList
     * : expression (',' expression)*
     * ;
     * <p>
     * expression
     * : expression bop='.'( IDENTIFIER | functionCall | THIS )
     *
     * @param ctx
     */
    private void handleOtherFunctionCall(FunctionCallContext ctx) {
        String idName = ctx.IDENTIFIER().getText();

        // 获得参数类型，这些类型已经在表达式中推断出来
        List<Type> paramTypes = getParamTypes(ctx);

        boolean found = false;

        // 看看是不是点符号表达式调用的，调用的是类的方法
        if (ctx.parent instanceof ExpressionContext) {
            ExpressionContext exp = (ExpressionContext) ctx.parent;
            // 检查上一级的节点 是否是一个点的调用
            // expression : expression bop='.'
            // a.doSth(c,d)
            // expression
            // ->expression bop='.' functionCall <----- ctx  <-----exp
            // ->expression bop='.' IDENTIFIER '(' expressionList? ')'
            if (null != exp.bop) {
                found = whetherFindClassFunctionCall(ctx, idName, paramTypes);
            }
        }

        Scope scope = at.enclosingScopeOfNode(ctx);

        //从当前 Scope 逐级查找函数(或方法)
        if (!found) {
            FunctionScope function = at.lookupFunction(scope, idName, paramTypes);
            if (null != function) {
                found = true;
                at.symbolOfNode.put(ctx, function);
                at.typeOfNode.put(ctx, function.returnType);
            }
        }

        if (!found) {
            ClassScopeAndType theClass = at.lookupClass(scope, idName);
            if (null != theClass) {
                // 是类的构建函数，用相同的名称查找一个 class
                FunctionScope function = theClass.findConstructor(paramTypes);
                if (null != function) {
                    found = true;
                    at.symbolOfNode.put(ctx, function);
                }
                //如果是与类名相同的方法，并且没有参数，那么就是缺省构造方法
                else if (null == ctx.expressionList()) {
                    found = true;
                    // TODO 直接赋予class
                    at.symbolOfNode.put(ctx, theClass);
                } else {
                    at.log("unknown class constructor: " + ctx.getText(), ctx);
                }

                at.typeOfNode.put(ctx, theClass); // 这次函数调用是返回一个对象
            } else {
                //是一个函数型的变量
                Variable variable = at.lookupFunctionVariable(scope, idName, paramTypes);
                if (variable != null && variable.type instanceof FunctionType) {
                    found = true;
                    at.symbolOfNode.put(ctx, variable);
                    at.typeOfNode.put(ctx, variable.type);
                } else {
                    at.log("unknown function or function variable: " + ctx.getText(), ctx);
                }
            }

        }
    }

    private boolean whetherFindClassFunctionCall(FunctionCallContext ctx, String idName, List<Type> paramTypes) {
        boolean found = false;
        ExpressionContext exp = (ExpressionContext) ctx.parent;
        final int bopType = exp.bop.getType();
        switch (bopType) {
            case PlayScriptParser.DOT:
                Symbol symbol = at.symbolOfNode.get(exp.expression(0));
                //在第二遍扫描中 enterVariableDeclaratorId 将 Variable 放入了 at.symbolOfNode 中
                if (symbol instanceof Variable && ((Variable) symbol).type instanceof ClassScopeAndType) {
                    ClassScopeAndType theClass = (ClassScopeAndType) ((Variable) symbol).type;

                    //查找名称和参数类型都匹配的函数。不允许名称和参数都相同，但返回值不同的情况。
                    FunctionScope function = theClass.getFunction(idName, paramTypes);
                    if (null != function) {
                        found = true;
                        at.symbolOfNode.put(ctx, function);
                        at.typeOfNode.put(ctx, function.getReturnType());
                    } else {
                        Variable funcVar = theClass.getFunctionVariable(idName, paramTypes);
                        if (null != funcVar) {
                            found = true;
                            at.symbolOfNode.put(ctx, funcVar);
                            at.typeOfNode.put(ctx, ((FunctionType) funcVar.type).getReturnType());
                        } else {
                            at.log("unable to find method " + idName + " in Class " + theClass.name, exp);
                        }
                    }

                } else {
                    at.log("unable to resolve a class", ctx);
                }
                break;
            default:
                break;
        }
        return found;
    }

    /**
     * functionCall
     * : IDENTIFIER '(' expressionList? ')'
     * | THIS '(' expressionList? ')'
     * | SUPER '(' expressionList? ')'
     * ;
     * expressionList
     * : expression (',' expression)*
     * ;
     * 获得参数类型，这些类型已经在表达式中推断出来
     *
     * @param ctx
     * @return
     */
    private List<Type> getParamTypes(FunctionCallContext ctx) {
        List<Type> paramTypes = new LinkedList<>();
        Type type;
        final ExpressionListContext expressionListContext = ctx.expressionList();
        if (null != expressionListContext) {
            for (ExpressionContext expressionContext : expressionListContext.expression()) {
                type = at.typeOfNode.get(expressionContext);
                paramTypes.add(type);
            }
        }

        return paramTypes;
    }

    /**
     * expression是左递归的,需要消解处理点符号表达式的层层引用
     *
     * @param ctx
     */
    @Override
    public void exitExpression(ExpressionContext ctx) {
        Type type = null;
        if (null != ctx.bop) {
            final int bopType = ctx.bop.getType();
            switch (bopType) {
                case PlayScriptParser.DOT:
                    // 这是个左递归,要不断的把左边的节点的计算结果存到 symbolOfNode,所以要在 exitExpression 里操作
                    Symbol symbol = at.symbolOfNode.get(ctx.expression(0));
                    if (symbol instanceof Variable && ((Variable) symbol).type instanceof ClassScopeAndType) {
                        ClassScopeAndType theClass = (ClassScopeAndType) ((Variable) symbol).type;

                        //引用类的属性
                        if (null != ctx.IDENTIFIER()) {
                            String idName = ctx.IDENTIFIER().getText();
                            // 在类的scope里去查找,不需要改变当前的scope
                            Variable variable = at.lookupVariable(theClass, idName);
                            if (null != variable) {
                                at.symbolOfNode.put(ctx, variable);
                                //类型综合(冒泡)
                                type = variable.type;
                            } else {
                                at.log("unable to find field " + idName + " in Class " + theClass.name, ctx);
                            }
                        }

                        //引用类的方法
                        else if (null != ctx.functionCall()) {
                            type = at.typeOfNode.get(ctx.functionCall());
                        }

                    } else {
                        at.log("symbol is not a qualified object：" + symbol, ctx);
                    }

                    break;

                default:
                    break;
            }

        } else if (ctx.primary() != null) {
            //变量引用冒泡:
            //  如果下级节点是一个变量,冒泡向上级传递,以便在点符号表达式中使用
            //  也包括 This 和 Super 的冒泡
            Symbol symbol = at.symbolOfNode.get(ctx.primary());
            at.symbolOfNode.put(ctx, symbol);
        }

        //类型推断和综合
        if (ctx.primary() != null) {
            type = at.typeOfNode.get(ctx.primary());
        } else if (ctx.functionCall() != null) {
            type = at.typeOfNode.get(ctx.functionCall());
        } else if (ctx.bop != null && ctx.expression().size() >= 2) {
            Type type1 = at.typeOfNode.get(ctx.expression(0));
            Type type2 = at.typeOfNode.get(ctx.expression(1));

            // 语法规则: https://time.geekbang.org/column/article/132693
            switch (ctx.bop.getType()) {
                case PlayScriptParser.ADD:
                    if (type1 == PrimitiveType.String || type2 == PrimitiveType.String) {
                        type = PrimitiveType.String;
                    } else if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
                        //类型“向上”对齐，比如一个int和一个float，取float
                        type = PrimitiveType.getUpperType(type1, type2);
                    } else {
                        at.log("operand should be PrimitiveType for additive and multiplicative operation", ctx);
                    }
                    break;
                case PlayScriptParser.SUB:
                case PlayScriptParser.MUL:
                case PlayScriptParser.DIV:
                    if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
                        //类型“向上”对齐，比如一个int和一个float，取float
                        type = PrimitiveType.getUpperType(type1, type2);
                    } else {
                        at.log("operand should be PrimitiveType for additive and multiplicative operation", ctx);
                    }

                    break;
                case PlayScriptParser.EQUAL:
                case PlayScriptParser.NOTEQUAL:
                case PlayScriptParser.LE:
                case PlayScriptParser.LT:
                case PlayScriptParser.GE:
                case PlayScriptParser.GT:
                case PlayScriptParser.AND:
                case PlayScriptParser.OR:
                case PlayScriptParser.BANG:
                    type = PrimitiveType.Boolean;
                    break;
                case PlayScriptParser.ASSIGN:
                case PlayScriptParser.ADD_ASSIGN:
                case PlayScriptParser.SUB_ASSIGN:
                case PlayScriptParser.MUL_ASSIGN:
                case PlayScriptParser.DIV_ASSIGN:
                case PlayScriptParser.AND_ASSIGN:
                case PlayScriptParser.OR_ASSIGN:
                case PlayScriptParser.XOR_ASSIGN:
                case PlayScriptParser.MOD_ASSIGN:
                case PlayScriptParser.LSHIFT_ASSIGN:
                case PlayScriptParser.RSHIFT_ASSIGN:
                case PlayScriptParser.URSHIFT_ASSIGN:
                    type = type1;
                    break;
            }
        }

        //设置上一级节点的类型(类似于冒泡操作)
        at.typeOfNode.put(ctx, type);
    }


    /**
     * 对变量初始化部分也需要做类型推断
     *
     * @param ctx
     */
    @Override
    public void exitVariableInitializer(VariableInitializerContext ctx) {
        if (ctx.expression() != null) {
            at.typeOfNode.put(ctx, at.typeOfNode.get(ctx.expression()));
        }
    }

    /**
     * 根据字面量来推断类型
     *
     * @param ctx
     */
    @Override
    public void exitLiteral(LiteralContext ctx) {
        if (null != ctx.BOOL_LITERAL()) {
            at.typeOfNode.put(ctx, PrimitiveType.Boolean);
        } else if (null != ctx.CHAR_LITERAL()) {
            at.typeOfNode.put(ctx, PrimitiveType.Char);
        } else if (null != ctx.NULL_LITERAL()) {
            at.typeOfNode.put(ctx, PrimitiveType.Null);
        } else if (null != ctx.STRING_LITERAL()) {
            at.typeOfNode.put(ctx, PrimitiveType.String);
        } else if (null != ctx.integerLiteral()) {
            at.typeOfNode.put(ctx, PrimitiveType.Integer);
        } else if (null != ctx.floatLiteral()) {
            at.typeOfNode.put(ctx, PrimitiveType.Float);
        }
    }

    /**
     * 在结束扫描之前,把this()和super()构造函数消解掉
     *
     * @param ctx
     */
    @Override
    public void exitProg(ProgContext ctx) {
        for (FunctionCallContext fcc : thisConstructorList) {
            resolveThisConstructorCall(fcc);
        }

        for (FunctionCallContext fcc : superConstructorList) {
            resolveSuperConstructorCall(fcc);
        }
    }

    /**
     * 消解this()构造函数
     *
     * @param ctx
     */
    private void resolveThisConstructorCall(FunctionCallContext ctx) {
        ClassScopeAndType theClass = at.enclosingClassOfNode(ctx);
        if (theClass != null) {
            FunctionScope function = at.enclosingFunctionOfNode(ctx);
            if (null != function && function.isConstructor()) {
                List<Type> paramTypes = getParamTypes(ctx);
                FunctionScope refered = theClass.findConstructor(paramTypes);
                if (null != refered) {
                    at.symbolOfNode.put(ctx, refered);
                    at.typeOfNode.put(ctx, theClass);
                } else if (0 == paramTypes.size()) {
                    //缺省构造函数
                    at.symbolOfNode.put(ctx, refered);
                    at.typeOfNode.put(ctx, theClass);
                } else {
                    at.log("can not find a constructor matches this()", ctx);
                }
            } else {
                at.log("this() should only be called inside a class constructor", ctx);
            }
        } else {
            at.log("this() should only be called inside a class", ctx);
        }
    }

    /**
     * 消解Super()构造函数
     * TODO 对于调用super()是有要求的,比如:
     * TODO     (1)必须出现在构造函数的第一行
     * TODO     (2)this()和super()不能同时出现,等等
     *
     * @param ctx
     */
    private void resolveSuperConstructorCall(FunctionCallContext ctx) {
        ClassScopeAndType theClass = at.enclosingClassOfNode(ctx);
        if (null != theClass) {
            FunctionScope function = at.enclosingFunctionOfNode(ctx);
            if (null != function && function.isConstructor()) {
                ClassScopeAndType parentClass = theClass.getParentClass();
                if (parentClass != null) {
                    List<Type> paramTypes = getParamTypes(ctx);
                    FunctionScope refered = parentClass.findConstructor(paramTypes);
                    if (refered != null) {
                        at.symbolOfNode.put(ctx, refered);
                        at.typeOfNode.put(ctx, theClass);
                    } else if (0 == paramTypes.size()) {
                        //缺省构造函数
                        at.symbolOfNode.put(ctx, parentClass);
                        at.typeOfNode.put(ctx, theClass);
                    } else {
                        at.log("can not find a constructor matches this()", ctx);
                    }
                } else {
                    //父类是最顶层的基类。
                    //TODO 这里暂时不处理
                }
            } else {
                at.log("super() should only be called inside a class constructor", ctx);
            }
        } else {
            at.log("super() should only be called inside a class", ctx);
        }
    }
}
