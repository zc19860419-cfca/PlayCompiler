package play.compiler.script.compile;

import play.compiler.script.generate.PlayScriptBaseVisitor;
import play.compiler.script.generate.PlayScriptParser;
import play.compiler.script.generate.PlayScriptParser.*;
import play.compiler.script.object.FunctionObject;
import play.compiler.script.object.NullObject;
import play.compiler.script.object.StackFrame;
import play.compiler.script.runtime.LValue;
import play.compiler.script.runtime.PrimitiveType;
import play.compiler.script.runtime.Type;
import play.compiler.script.utils.LogicUtils;
import play.compiler.script.utils.NumberUtils;

import java.util.Stack;

/**
 * @Author: zhangchong
 * @Description:
 */
public class ASTEvaluator extends PlayScriptBaseVisitor<Object> {

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
        } else if (ctx.functionCall() != null) {
            rtn = visitFunctionCall(ctx.functionCall());
        }
        return rtn;
    }

    //=================================== 函数相关运算 ===================================
    private Object getLValue(Variable symbol) {
        return null;
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
