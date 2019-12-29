package play.compiler.script.compile;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.script.generate.PlayScriptBaseVisitor;
import play.compiler.script.generate.PlayScriptParser;
import play.compiler.script.generate.PlayScriptParser.BlockContext;
import play.compiler.script.generate.PlayScriptParser.BlockStatementContext;
import play.compiler.script.generate.PlayScriptParser.BlockStatementsContext;
import play.compiler.script.generate.PlayScriptParser.ClassBodyContext;
import play.compiler.script.generate.PlayScriptParser.ClassBodyDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.ClassDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.ConstructorDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.CreatorContext;
import play.compiler.script.generate.PlayScriptParser.EnhancedForControlContext;
import play.compiler.script.generate.PlayScriptParser.ExpressionContext;
import play.compiler.script.generate.PlayScriptParser.ExpressionListContext;
import play.compiler.script.generate.PlayScriptParser.FieldDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.FloatLiteralContext;
import play.compiler.script.generate.PlayScriptParser.ForControlContext;
import play.compiler.script.generate.PlayScriptParser.ForInitContext;
import play.compiler.script.generate.PlayScriptParser.FormalParameterContext;
import play.compiler.script.generate.PlayScriptParser.FormalParameterListContext;
import play.compiler.script.generate.PlayScriptParser.FormalParametersContext;
import play.compiler.script.generate.PlayScriptParser.FunctionBodyContext;
import play.compiler.script.generate.PlayScriptParser.FunctionCallContext;
import play.compiler.script.generate.PlayScriptParser.FunctionDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.IntegerLiteralContext;
import play.compiler.script.generate.PlayScriptParser.LiteralContext;
import play.compiler.script.generate.PlayScriptParser.MemberDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.ParExpressionContext;
import play.compiler.script.generate.PlayScriptParser.PrimaryContext;
import play.compiler.script.generate.PlayScriptParser.ProgContext;
import play.compiler.script.generate.PlayScriptParser.QualifiedNameContext;
import play.compiler.script.generate.PlayScriptParser.QualifiedNameListContext;
import play.compiler.script.generate.PlayScriptParser.StatementContext;
import play.compiler.script.generate.PlayScriptParser.SuperSuffixContext;
import play.compiler.script.generate.PlayScriptParser.SwitchBlockStatementGroupContext;
import play.compiler.script.generate.PlayScriptParser.SwitchLabelContext;
import play.compiler.script.generate.PlayScriptParser.TypeArgumentContext;
import play.compiler.script.generate.PlayScriptParser.TypeListContext;
import play.compiler.script.generate.PlayScriptParser.TypeTypeContext;
import play.compiler.script.generate.PlayScriptParser.TypeTypeOrVoidContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorIdContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorsContext;
import play.compiler.script.generate.PlayScriptParser.VariableInitializerContext;
import play.compiler.script.generate.PlayScriptParser.VariableModifierContext;
import play.compiler.script.object.BreakObject;
import play.compiler.script.object.ClassObject;
import play.compiler.script.object.FunctionObject;
import play.compiler.script.object.NullObject;
import play.compiler.script.object.PlayObject;
import play.compiler.script.object.ReturnObject;
import play.compiler.script.object.StackFrame;
import play.compiler.script.runtime.LValue;
import play.compiler.script.runtime.PrimitiveType;
import play.compiler.script.runtime.Type;
import play.compiler.script.utils.LogicUtils;
import play.compiler.script.utils.NumberUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * @Author: zhangchong
 * @Description:
 */
public class ASTEvaluator extends PlayScriptBaseVisitor<Object> {
    private static Logger LOG = LoggerFactory.getLogger(ASTEvaluator.class);
    protected boolean traceStackFrame = false;
    protected boolean traceFunctionCall = false;
    /**
     * 之前的编译结果
     */
    private AnnotatedTree at = null;
    /**
     * 栈桢的管理
     */
    private Stack<StackFrame> stack = new Stack<StackFrame>();

    /**
     * 堆，用于保存对象
     *
     * @param at
     */
    public ASTEvaluator(AnnotatedTree at) {
        this.at = at;
    }

    /**
     * 栈桢入栈。
     * 其中最重要的任务,是要保证栈桢的 parentFrame 设置正确.否则,
     * (1)随着栈的变深,查找变量的性能会降低;
     * (2)甚至有可能找错栈桢,比如在递归(直接或间接)的场景下.
     * <p>
     * Case1:[兄弟关系]如果新加入的栈帧,跟某个已有的栈帧的 enclosingScope 是一样的,
     * 那么这俩的 parentFrame 也是一样的.
     * 比如:
     * void foo(){};
     * void bar(foo());
     * <p>
     * 或者:
     * void foo();
     * if(...){
     * foo();
     * }
     * <p>
     * Case2:[父子关系]如果新加入的栈桢,是某个已有的栈桢的下一级,
     * 那么就把把这个父子关系建立起来.
     * 比如:
     * void foo(){
     * if (...){  //把这个块往栈桢里加的时候,就符合这个条件.
     * }
     * }
     * 再比如,下面的例子:
     * class MyClass{
     * void foo();
     * }
     * MyClass c = MyClass();  //先加Class的栈桢,里面有类的属性,包括父类的
     * c.foo();                //再加foo()的栈桢
     * <p>
     * Case3:[函数赋值]这是针对函数可能是一等公民的情况.
     * 这时函数运行时的作用域,与声明时的作用域会不一致.
     * 我在这里设计了一个 "receiver" 的机制,意思是这个函数是被哪个变量接收了.
     * 要按照这个receiver的作用域来判断.
     *
     * @param newFrame 新加入的栈帧
     */
    private void pushStack(StackFrame newFrame) {
        if (0 < stack.size()) {
            // 从栈顶到栈底依次查找
            for (StackFrame existFrame : stack) {
                // Case1
                if (existFrame.getScope().getEnclosingScope() == newFrame.getScope().getEnclosingScope()) {
                    newFrame.setParentFrame(existFrame.getParentFrame());
                    break;
                }

                // Case2
                if (existFrame.getScope() == newFrame.getScope().getEnclosingScope()) {
                    newFrame.setParentFrame(existFrame);
                    break;
                }

                // Case3
                final PlayObject object = newFrame.getObject();
                if (object instanceof FunctionObject) {
                    FunctionObject functionObject = (FunctionObject) object;
                    final Variable receiver = functionObject.getReceiver();
                    if (null != receiver &&
                            receiver.enclosingScope == existFrame.getScope()) {
                        newFrame.setParentFrame(existFrame);
                        break;
                    }
                }
            }
            if (null == newFrame.getParentFrame()) {
                newFrame.setParentFrame(stack.peek());
            }
        }

        stack.push(newFrame);
        if (traceStackFrame) {
            dumpStackFrame();
        }
    }

    /**
     * 弹出栈顶栈帧
     */
    private void popStack() {
        stack.pop();
    }

    private void dumpStackFrame() {
        LOG.debug("Stack Frames ----------------");
        for (StackFrame frame : stack) {
            LOG.debug(frame.toString());
        }
        LOG.debug("-----------------------------");
    }

    @Override
    public Object visitProg(ProgContext ctx) {
        Object rtn = null;
        pushStack(new StackFrame((BlockScope) at.node2Scope.get(ctx)));

        rtn = visitBlockStatements(ctx.blockStatements());

        popStack();

        return rtn;
    }

    ////////////////////////////////////////////////////////////
    /// visit每个节点

    /**
     * block
     * : '{' blockStatements '}'
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitBlock(BlockContext ctx) {

        BlockScope scope = (BlockScope) at.node2Scope.get(ctx);
        if (scope != null) {
            //有些block是不对应scope的,比如函数底下的 block.
            StackFrame frame = new StackFrame(scope);
            // frame.parentFrame = stack.peek();
            pushStack(frame);
        }


        Object rtn = visitBlockStatements(ctx.blockStatements());

        if (scope != null) {
            popStack();
        }

        return rtn;
    }

    /**
     * blockStatements
     * : blockStatement*
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitBlockStatements(BlockStatementsContext ctx) {
        Object rtn = null;
        for (BlockStatementContext childBlock : ctx.blockStatement()) {
            rtn = visitBlockStatement(childBlock);
            if (rtn instanceof BreakObject) {
                //如果返回的是break,那么不执行下面的语句
                break;
            } else if (rtn instanceof ReturnObject) {
                //碰到 Return,退出函数
                // TODO 要能层层退出一个个block，弹出一个栈桢
                break;
            }
        }
        return rtn;
    }

    /**
     * blockStatement
     * : variableDeclarators ';'
     * | statement
     * | functionDeclaration
     * | classDeclaration
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitBlockStatement(BlockStatementContext ctx) {
        Object rtn = null;
        if (null != ctx.variableDeclarators()) {
            rtn = visitVariableDeclarators(ctx.variableDeclarators());
        } else if (null != ctx.statement()) {
            rtn = visitStatement(ctx.statement());
        }
        return rtn;
    }

    @Override
    public Object visitStatement(StatementContext ctx) {
        Object rtn = null;
        if (ctx.statementExpression != null) {
            rtn = visitExpression(ctx.statementExpression);
        } else if (ctx.IF() != null) {
            Boolean condition = (Boolean) visitParExpression(ctx.parExpression());
            if (Boolean.TRUE == condition) {
                rtn = visitStatement(ctx.statement(0));
            } else if (ctx.ELSE() != null) {
                rtn = visitStatement(ctx.statement(1));
            }
        }

        //while循环
        else if (ctx.WHILE() != null) {
            if (ctx.parExpression().expression() != null && ctx.statement(0) != null) {

                while (true) {
                    //每次循环都要计算一下循环条件
                    Boolean condition = true;
                    Object value = visitExpression(ctx.parExpression().expression());
                    if (value instanceof LValue) {
                        condition = (Boolean) ((LValue) value).getValue();
                    } else {
                        condition = (Boolean) value;
                    }

                    if (condition) {
                        //执行while后面的语句
                        if (condition) {
                            rtn = visitStatement(ctx.statement(0));

                            //break
                            if (rtn instanceof BreakObject) {
                                rtn = null;  //清除BreakObject，也就是只跳出一层循环
                                break;
                            }
                            //return
                            else if (rtn instanceof ReturnObject) {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

        }

        //for循环
        else if (ctx.FOR() != null) {
            // 添加StackFrame
            BlockScope scope = (BlockScope) at.node2Scope.get(ctx);
            StackFrame frame = new StackFrame(scope);
            // frame.parentFrame = stack.peek();
            pushStack(frame);

            ForControlContext forControl = ctx.forControl();
            if (forControl.enhancedForControl() != null) {

            } else {
                // 初始化部分执行一次
                if (forControl.forInit() != null) {
                    rtn = visitForInit(forControl.forInit());
                }

                while (true) {
                    Boolean condition = true; // 如果没有条件判断部分，意味着一直循环
                    if (forControl.expression() != null) {
                        Object value = visitExpression(forControl.expression());
                        if (value instanceof LValue) {
                            condition = (Boolean) ((LValue) value).getValue();
                        } else {
                            condition = (Boolean) value;
                        }
                    }

                    if (condition) {
                        // 执行for的语句体
                        rtn = visitStatement(ctx.statement(0));

                        //处理break
                        if (rtn instanceof BreakObject) {
                            rtn = null;
                            break;
                        }
                        //return
                        else if (rtn instanceof ReturnObject) {
                            break;
                        }

                        // 执行forUpdate，通常是“i++”这样的语句。这个执行顺序不能出错。
                        if (forControl.forUpdate != null) {
                            visitExpressionList(forControl.forUpdate);
                        }
                    } else {
                        break;
                    }
                }
            }

            // 去掉StackFrame
            popStack();
        }

        //block
        else if (ctx.blockLabel != null) {
            rtn = visitBlock(ctx.blockLabel);

        }

        //break语句
        else if (ctx.BREAK() != null) {
            rtn = BreakObject.instance();
        }

        //return语句
        else if (ctx.RETURN() != null) {
            if (ctx.expression() != null) {
                rtn = visitExpression(ctx.expression());

                //return语句应该不需要左值
                // TODO 取左值的场景需要优化,目前都是取左值.
                if (rtn instanceof LValue) {
                    rtn = ((LValue) rtn).getValue();
                }

                // 把闭包涉及的环境变量都打包带走
                if (rtn instanceof FunctionObject) {
                    FunctionObject functionObject = (FunctionObject) rtn;
                    getClosureValues(functionObject.getFunctionScope(), functionObject);
                }
                // 如果返回的是一个对象,那么检查它的所有属性里有没有是闭包的.
                // TODO 如果属性仍然是一个对象，可能就要向下递归查找了。
                else if (rtn instanceof ClassObject) {
                    ClassObject classObject = (ClassObject) rtn;
                    getClosureValues(classObject);
                }

            }

            //把真实的返回值封装在一个ReturnObject对象里，告诉visitBlockStatements停止执行下面的语句
            rtn = new ReturnObject(rtn);
        }
        return rtn;
    }

    @Override
    public Object visitEnhancedForControl(EnhancedForControlContext ctx) {
        return super.visitEnhancedForControl(ctx);
    }

    /**
     * int a=10,b = {1,2,2,3}
     * variableDeclarators
     * : typeType variableDeclarator (',' variableDeclarator)*
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitVariableDeclarators(VariableDeclaratorsContext ctx) {
        Object rtn = null;
        //后面要利用这个类型信息
        // Integer typeType = (Integer)visitTypeType(ctx.typeType());
        for (VariableDeclaratorContext child : ctx.variableDeclarator()) {
            rtn = visitVariableDeclarator(child);
        }
        return rtn;
    }

    /**
     * a=10
     * b = {1,2,2,3}
     * variableDeclarator
     * : variableDeclaratorId ('=' variableInitializer)?
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitVariableDeclarator(VariableDeclaratorContext ctx) {
        Object rtn = null;
        LValue lValue = (LValue) visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (null != ctx.variableInitializer()) {
            rtn = visitVariableInitializer(ctx.variableInitializer());
            if (rtn instanceof LValue) {
                rtn = ((LValue) rtn).getValue();
            }
            lValue.setValue(rtn);
        }
        return rtn;
    }

    /**
     * symbolOfNode(AST节点对应的Symbol) 会在第二遍以及第三遍扫描中 进行 put 操作
     * {@link TypeResolver}(把变量,类继承,函数声明的类型都解析出来) 以及
     * {@link RefResolver}(解析引用以及类型推断)
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        Symbol symbol = at.symbolOfNode.get(ctx);
        return getLValue((Variable) symbol);
    }

    /**
     * arrayInitializer: {1,2,2,3}
     * variableInitializer
     * : arrayInitializer
     * | expression
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitVariableInitializer(VariableInitializerContext ctx) {
        Object rtn = null;
        if (null != ctx.expression()) {
            rtn = visitExpression(ctx.expression());
        }
        return rtn;
    }

    @Override
    public Object visitExpression(ExpressionContext ctx) {
        Object rtn = null;
        if (ctx.bop != null && ctx.expression().size() >= 2) {
            Object left = visitExpression(ctx.expression(0));
            Object right = visitExpression(ctx.expression(1));
            Object leftObject = left;
            Object rightObject = right;

            if (left instanceof LValue) {
                leftObject = ((LValue) left).getValue();
            }

            if (right instanceof LValue) {
                rightObject = ((LValue) right).getValue();
            }

            /**
             * 本节点期待的数据类型
             */
            Type type = at.typeOfNode.get(ctx);

            /**
             * 左右两个子节点的类型
             */
            Type type1 = at.typeOfNode.get(ctx.expression(0));
            Type type2 = at.typeOfNode.get(ctx.expression(1));

            switch (ctx.bop.getType()) {
                case PlayScriptParser.ADD:
                    rtn = NumberUtils.add(leftObject, rightObject, type);
                    break;
                case PlayScriptParser.SUB:
                    rtn = NumberUtils.minus(leftObject, rightObject, type);
                    break;
                case PlayScriptParser.MUL:
                    rtn = NumberUtils.mul(leftObject, rightObject, type);
                    break;
                case PlayScriptParser.DIV:
                    rtn = NumberUtils.div(leftObject, rightObject, type);
                    break;
                case PlayScriptParser.EQUAL:
                    rtn = LogicUtils.EQ(leftObject, rightObject, PrimitiveType.getUpperType(type1, type2));
                    break;
                case PlayScriptParser.NOTEQUAL:
                    rtn = !LogicUtils.EQ(leftObject, rightObject, PrimitiveType.getUpperType(type1, type2));
                    break;
                case PlayScriptParser.LE:
                    rtn = LogicUtils.LE(leftObject, rightObject, PrimitiveType.getUpperType(type1, type2));
                    break;
                case PlayScriptParser.LT:
                    rtn = LogicUtils.LT(leftObject, rightObject, PrimitiveType.getUpperType(type1, type2));
                    break;
                case PlayScriptParser.GE:
                    rtn = LogicUtils.GE(leftObject, rightObject, PrimitiveType.getUpperType(type1, type2));
                    break;
                case PlayScriptParser.GT:
                    rtn = LogicUtils.GT(leftObject, rightObject, PrimitiveType.getUpperType(type1, type2));
                    break;

                case PlayScriptParser.AND:
                    rtn = (Boolean) leftObject && (Boolean) rightObject;
                    break;
                case PlayScriptParser.OR:
                    rtn = (Boolean) leftObject || (Boolean) rightObject;
                    break;
                case PlayScriptParser.ASSIGN:
                    if (left instanceof LValue) {
                        ((LValue) left).setValue(rightObject);
                        rtn = right;
                    } else {
                        System.out.println("Unsupported feature during assignment");
                    }
                    break;

                default:
                    break;
            }
        } else if (ctx.primary() != null) {
            rtn = visitPrimary(ctx.primary());
        } else if (ctx.postfix != null) {
            /**
             * 后缀运算，例如：i++ 或 i--
             */
            Object value = visitExpression(ctx.expression(0));
            LValue lValue = null;
            Type type = at.typeOfNode.get(ctx.expression(0));
            if (value instanceof LValue) {
                lValue = (LValue) value;
                value = lValue.getValue();
            }
            switch (ctx.postfix.getType()) {
                case PlayScriptParser.INC:
                    if (type == PrimitiveType.Integer) {
                        lValue.setValue((Integer) value + 1);
                    } else {
                        lValue.setValue((Long) value + 1);
                    }
                    rtn = value;
                    break;
                case PlayScriptParser.DEC:
                    if (type == PrimitiveType.Integer) {
                        lValue.setValue((Integer) value - 1);
                    } else {
                        lValue.setValue((long) value - 1);
                    }
                    rtn = value;
                    break;
                default:
                    break;
            }
        } else if (ctx.prefix != null) {
            /**
             * 前缀操作，例如：++i 或 --i
             */
            Object value = visitExpression(ctx.expression(0));
            LValue lValue = null;
            Type type = at.typeOfNode.get(ctx.expression(0));
            if (value instanceof LValue) {
                lValue = (LValue) value;
                value = lValue.getValue();
            }
            switch (ctx.prefix.getType()) {
                case PlayScriptParser.INC:
                    if (type == PrimitiveType.Integer) {
                        rtn = (Integer) value + 1;
                    } else {
                        rtn = (Long) value + 1;
                    }
                    lValue.setValue(rtn);
                    break;
                case PlayScriptParser.DEC:
                    if (type == PrimitiveType.Integer) {
                        rtn = (Integer) value - 1;
                    } else {
                        rtn = (Long) value - 1;
                    }
                    lValue.setValue(rtn);
                    break;
                //!符号，逻辑非运算
                case PlayScriptParser.BANG:
                    rtn = !((Boolean) value);
                    break;
                default:
                    break;
            }
        } else if (null != ctx.functionCall()) {
            rtn = visitFunctionCall(ctx.functionCall());
        }
        return rtn;
    }

    @Override
    public Object visitSuperSuffix(SuperSuffixContext ctx) {
        return super.visitSuperSuffix(ctx);
    }

    @Override
    public Object visitSwitchBlockStatementGroup(SwitchBlockStatementGroupContext ctx) {
        return super.visitSwitchBlockStatementGroup(ctx);
    }

    @Override
    public Object visitSwitchLabel(SwitchLabelContext ctx) {
        return super.visitSwitchLabel(ctx);
    }

    @Override
    public Object visitTypeType(TypeTypeContext ctx) {
        return visitPrimitiveType(ctx.primitiveType());
    }

    //=================================== 函数相关运算 ===================================

    /**
     * functionDeclaration
     * : typeTypeOrVoid? IDENTIFIER formalParameters ('[' ']')*
     * (THROWS qualifiedNameList)?
     * functionBody
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFunctionDeclaration(FunctionDeclarationContext ctx) {
        return visitFunctionBody(ctx.functionBody());
    }

    /**
     * functionBody
     * : block
     * | ';'
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFunctionBody(FunctionBodyContext ctx) {
        Object rtn = null;
        if (null != ctx.block()) {
            rtn = visitBlock(ctx.block());
        }
        return rtn;
    }

    /**
     * formalParameters
     * : '(' formalParameterList? ')'
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFormalParameters(FormalParametersContext ctx) {
        return super.visitFormalParameters(ctx);
    }

    /**
     * formalParameterList
     * : formalParameter (',' formalParameter)* (',' lastFormalParameter)?
     * | lastFormalParameter
     * ;
     * lastFormalParameter
     * : variableModifier* typeType '...' variableDeclaratorId
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFormalParameterList(FormalParameterListContext ctx) {
        return super.visitFormalParameterList(ctx);
    }

    /**
     * formalParameter
     * : variableModifier* typeType variableDeclaratorId
     * ;
     * <p>
     * variableModifier
     * : FINAL
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFormalParameter(FormalParameterContext ctx) {
        return super.visitFormalParameter(ctx);
    }

    /**
     * 处理 FunctionCall 节点
     * <p>
     * functionCall
     * : IDENTIFIER '(' expressionList? ')'
     * | THIS '(' expressionList? ')'
     * | SUPER '(' expressionList? ')'
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFunctionCall(FunctionCallContext ctx) {
        Object rtn = null;
        if (null != ctx.THIS()) {
            //this
            rtn = visitThisOrSuperFunctionCall(ctx);
        } else if (null != ctx.SUPER()) {
            //super:似乎跟this完全一样.因为方法的绑定是解析准确了的.
            rtn = visitThisOrSuperFunctionCall(ctx);
        } else {
            //FIXME:暂时不支持this和super
            //if (ctx.IDENTIFIER() == null) {return null;}

            rtn = visitIdentifierFunctionCall(ctx);
        }

        return rtn;
    }

    private Object visitIdentifierFunctionCall(FunctionCallContext ctx) {
        LOG.debug("visitIdentifierFunctionCall#Running...");
        LOG.debug("visitIdentifierFunctionCall#{}:" +
                        "startLine({}),positionInLine({})",
                ctx, ctx.getStart().getLine(), ctx.getStart().getStartIndex());
        Object rtn;
        //这是调用时的名称，不一定是真正的函数名，还可能是函数属性的变量名
        String functionName = ctx.IDENTIFIER().getText();

        Symbol symbol = at.symbolOfNode.get(ctx);
        if (symbol instanceof ClassScopeAndType) {
            //如果调用的是类的缺省构造函数，则直接创建对象并返回
            //类的缺省构造函数。没有一个具体函数跟它关联，只是指向了一个类。
            rtn = createAndInitClassObject((ClassScopeAndType) symbol);  //返回新创建的对象
        } else if (functionName.equals("println")) {
            //硬编码的一些函数
            // TODO 临时代码，用于打印输出
            println(ctx);
            rtn = null;
        } else {
            //在上下文中查找出函数,并根据需要创建 FunctionObject
            FunctionObject functionObject = getFuntionObject(ctx);
            FunctionScope functionScope = functionObject.getFunctionScope();

            //如果是对象的构造方法,则按照对象方法调用去执行,并返回所创建出的对象.
            if (functionScope.isConstructor()) {
                ClassScopeAndType theClass = (ClassScopeAndType) functionScope.enclosingScope;

                //先做缺省的初始化
                ClassObject newObject = createAndInitClassObject(theClass);

                methodCall(newObject, ctx, false);
                //返回新创建的对象
                rtn = newObject;
            } else {
                //计算参数值
                List<Object> paramValues = calcParamValues(ctx);

                if (traceFunctionCall) {
                    LOG.debug("visitIdentifierFunctionCall#FunctionCall : {}", ctx.getText());
                }
                rtn = functionCall(functionObject, paramValues);
            }
        }
        LOG.debug("visitIdentifierFunctionCall#Finished");
        return rtn;
    }

    private Object visitThisOrSuperFunctionCall(FunctionCallContext ctx) {
        thisConstructor(ctx);
        //不需要有返回值,因为本身就是在构造方法里调用的.
        return null;
    }

    /**
     * 对象方法调用.
     * 要先计算完参数的值,然后再添加对象的 StackFrame,然后再调用方法.
     *
     * @param classObject 实际调用时的对象.
     *                    通过这个对象可以获得真实的类,支持多态.
     * @param ctx
     * @param isSuper     是否已经是父类中的方法
     * @return
     */
    private Object methodCall(ClassObject classObject, FunctionCallContext ctx, boolean isSuper) {
        Object rtn = null;

        //查找函数,并根据需要创建 FunctionObject
        //如果查找到的是类的属性,FunctionType型的,需要把在对象的栈桢里查.
        StackFrame classFrame = new StackFrame(classObject);
        pushStack(classFrame);

        FunctionObject funtionObject = getFuntionObject(ctx);

        popStack();

        FunctionScope function = funtionObject.getFunctionScope();

        //对普通的类方法,需要在运行时动态绑定
        //这是从对象获得的类型,是真实类型.可能是变量声明时的类型的子类
        ClassScopeAndType theClass = classObject.getType();
        if (!function.isConstructor() && !isSuper) {
            //从当前类逐级向上查找,找到正确的方法定义
            FunctionScope overrided = theClass.getFunction(function.name, function.getParamTypes());
            //原来这个function,可能指向一个父类的实现.现在从子类中可能找到重载后的方法,这个时候要绑定到子类的方法上
            if (overrided != null && overrided != function) {
                function = overrided;
                funtionObject.setFunctionScope(function);
            }
        }

        //计算参数值
        List<Object> paramValues = calcParamValues(ctx);

        //对象的 frame 要等到函数参数都计算完了才能添加.
        pushStack(classFrame);

        //执行函数
        rtn = functionCall(funtionObject, paramValues);

        //弹出栈桢
        popStack();

        return rtn;
    }

    /**
     * 执行一个函数的方法体.需要首先设置参数值,然后再执行代码
     *
     * @param functionObject
     * @param paramValues
     * @return
     */
    private Object functionCall(FunctionObject functionObject, List<Object> paramValues) {
        Object result = null;

        //添加函数的栈帧
        StackFrame functionFrame = new StackFrame(functionObject);
        pushStack(functionFrame);

        /**
         * 给参数赋值,这些值进入 functionFrame
         * int foo(final int a, final int b)
         * functionDeclaration
         *     : typeTypeOrVoid? IDENTIFIER formalParameters ('[' ']')*
         *       (THROWS qualifiedNameList)?
         *       functionBody
         *     ;
         * formalParameters
         *     : '(' formalParameterList? ')'
         *     ;
         *
         * formalParameterList
         *     : formalParameter (',' formalParameter)* (',' lastFormalParameter)?
         *     | lastFormalParameter
         *     ;
         *
         * formalParameter
         *     : variableModifier* typeType variableDeclaratorId
         *     ;
         *
         * lastFormalParameter
         *     : variableModifier* typeType '...' variableDeclaratorId
         *     ;
         * variableModifier
         *     : FINAL
         *     ;
         */
        FunctionDeclarationContext functionCode = (FunctionDeclarationContext) functionObject.getFunctionScope().getParserRuleContext();
        final FormalParameterListContext formalParameterListContext = functionCode.formalParameters().formalParameterList();
        if (null != formalParameterListContext) {
            FormalParameterContext param;
            LValue lValue;
            for (int i = 0; i < formalParameterListContext.formalParameter().size(); i++) {
                param = formalParameterListContext.formalParameter(i);
                lValue = (LValue) visitVariableDeclaratorId(param.variableDeclaratorId());
                lValue.setValue(paramValues.get(i));
            }
        }

        //调用函数(方法)体
        result = visitFunctionDeclaration(functionCode);
        //从函数的栈桢中弹出 StackFrame
        popStack();

        if (result instanceof ReturnObject) {
            result = ((ReturnObject) result).getReturnValue();
        }

        return result;
    }

    /**
     * 计算某个函数调用时的参数值
     *
     * @param ctx
     * @return
     */
    private List<Object> calcParamValues(FunctionCallContext ctx) {
        List<Object> paramValues = new LinkedList<>();
        final ExpressionListContext expressionListContext = ctx.expressionList();
        if (null != expressionListContext) {
            final List<ExpressionContext> expressions = expressionListContext.expression();
            Object value;
            for (ExpressionContext exp : expressions) {
                value = visitExpression(exp);
                if (value instanceof LValue) {
                    value = ((LValue) value).getValue();
                }
                paramValues.add(value);
            }
        }

        return paramValues;
    }

    /**
     * 根据函数调用上下文,返回一个 FunctionObject
     * Case1:对于函数类型的变量,这个 FunctionObject 是存在变量里的;
     * Case2:对于普通的函数调用,此时需要创建一个.
     *
     * @param ctx
     * @return
     */
    private FunctionObject getFuntionObject(FunctionCallContext ctx) {
        FunctionObject result = null;
        if (null == ctx.IDENTIFIER()) {
            //暂时不支持 this 和 super
            result = null;
        } else {
            FunctionScope functionScope = null;
            final Symbol symbol = at.symbolOfNode.get(ctx);

            if (symbol instanceof Variable) {
                //Case1:函数类型的函数
                Variable variable = (Variable) symbol;
                final LValue lValue = getLValue(variable);
                final Object value = lValue.getValue();
                if (value instanceof FunctionObject) {
                    result = (FunctionObject) value;
                    functionScope = result.getFunctionScope();
                }
            } else if (symbol instanceof FunctionScope) {
                //Case2:普通的函数
                functionScope = (FunctionScope) symbol;
            } else {
                //Case3:报错
                //这是调用时的名称,不一定是真正的函数名,还可能是函数类型的变量名
                String functionName = ctx.IDENTIFIER().getText();
                at.log("unable to find function or function variable " + functionName, ctx);
                LOG.error("getFuntionObject#unable to find function or function variable {}", functionName);
                result = null;
            }

            if (null == result) {
                result = new FunctionObject(functionScope);
            }
        }
        return result;
    }

    private void thisConstructor(FunctionCallContext ctx) {
        Symbol symbol = at.symbolOfNode.get(ctx);
        if (symbol instanceof ClassScopeAndType) {
            //缺省构造函数
            //这里不用管,因为缺省构造函数一定会被调用
            return;
        } else if (symbol instanceof FunctionScope) {
            FunctionScope function = (FunctionScope) symbol;
            FunctionObject functionObject = new FunctionObject(function);

            List<Object> paramValues = calcParamValues(ctx);

            functionCall(functionObject, paramValues);
        }
    }

    ///////////////////////////////////////////////////////////
    /// 对象初始化

    /**
     * 从父类到子类层层执行缺省的初始化方法,即不带参数的初始化方法
     *
     * @param theClass
     * @return
     */
    protected ClassObject createAndInitClassObject(ClassScopeAndType theClass) {
        ClassObject obj = new ClassObject();
        obj.setType(theClass);

        Stack<ClassScopeAndType> ancestorChain = new Stack<>();

        // 从上到下执行缺省的初始化方法
        ancestorChain.push(theClass);
        while (theClass.getParentClass() != null) {
            ancestorChain.push(theClass.getParentClass());
            theClass = theClass.getParentClass();
        }

        // 执行缺省的初始化方法
        StackFrame frame = new StackFrame(obj);
        pushStack(frame);

        ClassScopeAndType c;
        while (ancestorChain.size() > 0) {
            c = ancestorChain.pop();
            defaultObjectInit(c, obj);
        }
        popStack();

        return obj;
    }

    /**
     * 类的缺省初始化方法
     *
     * @param theClass
     * @param obj
     */
    protected void defaultObjectInit(ClassScopeAndType theClass, ClassObject obj) {
        for (Symbol symbol : theClass.symbols) {
            // 把变量加到 obj 里,缺省先都初始化成 null,不允许有未初始化的
            if (symbol instanceof Variable) {
                obj.getFields().put((Variable) symbol, null);
            }
        }

        // 执行缺省初始化
        ClassBodyContext ctx = ((ClassDeclarationContext) theClass.ctx).classBody();
        visitClassBody(ctx);
    }

    ///////////////////////////////////////////////////////////
    /// 类相关语句
//    visitClassBody
//            visitClassBodyDeclaration
//            visitConstructorDeclaration
//    visitCreator


    /**
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
    @Override
    public Object visitClassDeclaration(ClassDeclarationContext ctx) {
        return super.visitClassDeclaration(ctx);
    }

    /**
     * classBody
     * : '{' classBodyDeclaration* '}'
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitClassBody(ClassBodyContext ctx) {
        Object rtn = null;
        for (ClassBodyDeclarationContext childClassBodyDeclaration : ctx.classBodyDeclaration()) {
            rtn = visitClassBodyDeclaration(childClassBodyDeclaration);
        }
        return rtn;
    }

    /**
     * classBodyDeclaration
     * : ';'
     * | memberDeclaration
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitClassBodyDeclaration(ClassBodyDeclarationContext ctx) {
        Object rtn = null;
        if (null != ctx.memberDeclaration()) {
            rtn = visitMemberDeclaration(ctx.memberDeclaration());
        }
        return rtn;
    }

    /**
     * memberDeclaration
     * : functionDeclaration
     * | fieldDeclaration
     * | classDeclaration //内部类的语法结构
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitMemberDeclaration(MemberDeclarationContext ctx) {
        Object rtn = null;
        if (null != ctx.fieldDeclaration()) {
            rtn = visitFieldDeclaration(ctx.fieldDeclaration());
        }
        return rtn;
    }

    /**
     * fieldDeclaration
     * : variableDeclarators ';'
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitFieldDeclaration(FieldDeclarationContext ctx) {
        Object rtn = null;
        if (null != ctx.variableDeclarators()) {
            rtn = visitVariableDeclarators(ctx.variableDeclarators());
        }
        return rtn;
    }

    /**
     * constructorDeclaration
     * : IDENTIFIER formalParameters (THROWS qualifiedNameList)? constructorBody=block
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitConstructorDeclaration(ConstructorDeclarationContext ctx) {
        return super.visitConstructorDeclaration(ctx);
    }

    /**
     * creator
     * : IDENTIFIER arguments
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitCreator(CreatorContext ctx) {
        return super.visitCreator(ctx);
    }

    ///////////////////////////////////////////////////////////

    /**
     * typeArgument
     * : typeType
     * | '?' ((EXTENDS | SUPER) typeType)?
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitTypeArgument(TypeArgumentContext ctx) {
        return super.visitTypeArgument(ctx);
    }

    /**
     * typeList
     * : typeType (',' typeType)*
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitTypeList(TypeListContext ctx) {
        return super.visitTypeList(ctx);
    }

    /**
     * variableModifier
     * : FINAL
     * ;
     *
     * @param ctx
     * @return
     */
    @Override
    public Object visitVariableModifier(VariableModifierContext ctx) {
        return super.visitVariableModifier(ctx);
    }

    @Override
    public Object visitQualifiedName(QualifiedNameContext ctx) {
        return super.visitQualifiedName(ctx);
    }

    @Override
    public Object visitQualifiedNameList(QualifiedNameListContext ctx) {
        return super.visitQualifiedNameList(ctx);
    }

    @Override
    public Object visitTypeTypeOrVoid(TypeTypeOrVoidContext ctx) {
        return super.visitTypeTypeOrVoid(ctx);
    }

    ///////////////////////////////////////////////
    //为闭包获取环境变量的值

    /**
     * 为闭包获取环境变量的值
     *
     * @param function       闭包所关联的函数.这个函数会访问一些环境变量.
     * @param valueContainer 存放环境变量的值的容器
     */
    private void getClosureValues(FunctionScope function, PlayObject valueContainer) {
        if (function.closureVariables != null) {
            for (Variable var : function.closureVariables) {
                // 现在还可以从栈里取,退出函数以后就不行了
                LValue lValue = getLValue(var);
                Object value = lValue.getValue();
                valueContainer.getFields().put(var, value);
            }
        }
    }

    /**
     * 为从函数中返回的对象设置闭包值.
     * 因为多个函数型属性可能共享值,所以要打包到ClassObject中,而不是functionObject中
     *
     * @param classObject
     */
    private void getClosureValues(ClassObject classObject) {
        //TODO
//        //先放在一个临时对象里，避免对classObject即读又写
//        PlayObject tempObject = new PlayObject();
//        for ( Variable v : classObject.fields.keySet()) {
//            if (v.type instanceof FunctionType) {
//                Object object = classObject.fields.get(v);
//                if (object != null) {
//                    FunctionObject functionObject = (FunctionObject) object;
//                    getClosureValues(functionObject.function, tempObject);
//                }
//            }
//        }
//
//        classObject.fields.putAll(tempObject.fields);
    }


    ///////////////////////////////////////////////////////////
    /// 内置函数

    /**
     * 自己硬编码的println方法
     *
     * @param ctx
     */
    private void println(FunctionCallContext ctx) {
        if (ctx.expressionList() != null) {
            Object value = visitExpressionList(ctx.expressionList());
            if (value instanceof LValue) {
                value = ((LValue) value).getValue();
            }
            LOG.info(value.toString());
        } else {
            LOG.info("");
        }
    }

    /**
     * 从栈中取出已知的 StackFrame 对象来构建左值
     * 左值需要 (Variable,PlayObject)构建
     * 1.Variable: 传入的Variable
     * 2.PlayObject: 从栈上找到 Variable对应的 StackFrame,再从中取出 PlayObject
     * <p>
     * stack 通过运行时调用以下三种方法进行 pushStack
     * createAndInitClassObject, visitBlock, visitProg
     * push的栈帧对象 是由 该栈帧所属的类作用域以及类实际存放的变量构成
     * StackFrame:=(Scope, ParentFrame, PlayObject)
     * 其中 StackFrame parentFrame 是由 pushStack 时运行时绑定的
     *
     * @param variable
     * @return
     */
    private LValue getLValue(Variable variable) {
        StackFrame f = stack.peek();

        PlayObject valueContainer = null;
        Scope scope;
        while (f != null) {
            scope = f.getScope();
            if (scope.containsSymbol(variable)) {
                //对于对象来说,会查找所有父类的属性
                valueContainer = f.getObject();
                break;
            }
            f = f.getParentFrame();
        }

        //通过正常的作用域找不到,就从闭包里找
        //原理:PlayObject中可能有一些变量,其作用域跟 StackFrame.scope 是不同的.
        if (valueContainer == null) {
            f = stack.peek();
            while (f != null) {
                if (f.contains(variable)) {
                    valueContainer = f.getObject();
                    break;
                }
                f = f.getParentFrame();
            }
        }

        MyLValue lvalue = new MyLValue(valueContainer, variable);

        return lvalue;
    }

    /**
     * 自定义的左值对象
     */
    private final class MyLValue implements LValue {
        /**
         * 左值对象的变量值
         */
        private Variable variable;
        /**
         * 左值对象所属的容器对象
         */
        private PlayObject valueContainer;

        public MyLValue(PlayObject valueContainer, Variable variable) {
            this.valueContainer = valueContainer;
            this.variable = variable;
        }

        @Override
        public Object getValue() {
            //对于this或super关键字，直接返回这个对象，应该是ClassObject
            if (variable instanceof This || variable instanceof Super) {
                return valueContainer;
            }

            return valueContainer.getValue(variable);
        }

        @Override
        public void setValue(Object value) {
            valueContainer.setValue(variable, value);

            //如果variable是函数型变量,那改变functionObject.receiver
            if (value instanceof FunctionObject) {
                ((FunctionObject) value).setReceiver(variable);
            }
        }

        @Override
        public Variable getVariable() {
            return variable;
        }

        @Override
        public String toString() {
            return "LValue of " + variable.name + " : " + getValue();
        }

        @Override
        public PlayObject getValueContainer() {
            return valueContainer;
        }
    }


    @Override
    public Object visitExpressionList(ExpressionListContext ctx) {
        Object rtn = null;
        for (ExpressionContext child : ctx.expression()) {
            rtn = visitExpression(child);
        }
        return rtn;
    }

    @Override
    public Object visitForInit(ForInitContext ctx) {
        Object rtn = null;
        if (ctx.variableDeclarators() != null) {
            rtn = visitVariableDeclarators(ctx.variableDeclarators());
        } else if (ctx.expressionList() != null) {
            rtn = visitExpressionList(ctx.expressionList());
        }
        return rtn;
    }

    @Override
    public Object visitLiteral(LiteralContext ctx) {
        Object rtn = null;

        //整数
        if (ctx.integerLiteral() != null) {
            rtn = visitIntegerLiteral(ctx.integerLiteral());
        }

        //浮点数
        else if (ctx.floatLiteral() != null) {
            rtn = visitFloatLiteral(ctx.floatLiteral());
        }

        //布尔值
        else if (ctx.BOOL_LITERAL() != null) {
            if (ctx.BOOL_LITERAL().getText().equals("true")) {
                rtn = Boolean.TRUE;
            } else {
                rtn = Boolean.FALSE;
            }
        }

        //字符串
        else if (ctx.STRING_LITERAL() != null) {
            String withQuotationMark = ctx.STRING_LITERAL().getText();
            rtn = withQuotationMark.substring(1, withQuotationMark.length() - 1);
        }

        //单个的字符
        else if (ctx.CHAR_LITERAL() != null) {
            rtn = ctx.CHAR_LITERAL().getText().charAt(0);
        }

        //null字面量
        else if (ctx.NULL_LITERAL() != null) {
            rtn = NullObject.instance();
        }

        return rtn;
    }

    @Override
    public Object visitIntegerLiteral(IntegerLiteralContext ctx) {
        Object rtn = null;
        if (ctx.DECIMAL_LITERAL() != null) {
            rtn = Integer.valueOf(ctx.DECIMAL_LITERAL().getText());
        }
        return rtn;
    }

    @Override
    public Object visitFloatLiteral(FloatLiteralContext ctx) {
        return Float.valueOf(ctx.getText());
    }

    @Override
    public Object visitParExpression(ParExpressionContext ctx) {
        return visitExpression(ctx.expression());
    }

    @Override
    public Object visitPrimary(PrimaryContext ctx) {
        Object rtn = null;
        //字面量
        if (ctx.literal() != null) {
            rtn = visitLiteral(ctx.literal());
        }
        //变量
        else if (ctx.IDENTIFIER() != null) {
            Symbol symbol = at.symbolOfNode.get(ctx);
            if (symbol instanceof Variable) {
                rtn = getLValue((Variable) symbol);
            } else if (symbol instanceof FunctionScope) {
                FunctionObject obj = new FunctionObject((FunctionScope) symbol);
                rtn = obj;
            }
        }
        //括号括起来的表达式
        else if (ctx.expression() != null) {
            rtn = visitExpression(ctx.expression());
        }
        //this
        else if (ctx.THIS() != null) {
            This thisRef = (This) at.symbolOfNode.get(ctx);
            rtn = getLValue(thisRef);
        }
        //super
        else if (ctx.SUPER() != null) {
            Super superRef = (Super) at.symbolOfNode.get(ctx);
            rtn = getLValue(superRef);
        }

        return rtn;
    }

    @Override
    public Object visitPrimitiveType(PlayScriptParser.PrimitiveTypeContext ctx) {
        Object rtn = null;
        if (ctx.INT() != null) {
            rtn = PlayScriptParser.INT;
        } else if (ctx.LONG() != null) {
            rtn = PlayScriptParser.LONG;
        } else if (ctx.FLOAT() != null) {
            rtn = PlayScriptParser.FLOAT;
        } else if (ctx.DOUBLE() != null) {
            rtn = PlayScriptParser.DOUBLE;
        } else if (ctx.BOOLEAN() != null) {
            rtn = PlayScriptParser.BOOLEAN;
        } else if (ctx.CHAR() != null) {
            rtn = PlayScriptParser.CHAR;
        } else if (ctx.SHORT() != null) {
            rtn = PlayScriptParser.SHORT;
        } else if (ctx.BYTE() != null) {
            rtn = PlayScriptParser.BYTE;
        }
        return rtn;
    }
}
