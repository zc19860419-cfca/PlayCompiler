package play.compiler;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.ast.*;
import play.compiler.ast.node.ASTNode;
import play.compiler.ast.node.ASTNodeType;
import play.compiler.lexer.*;
import play.compiler.parser.SimpleParser;

/**
 * @Author: zhangchong
 * @Description: 实现一个计算器，但计算的结合性是有问题的。因为它使用了下面的语法规则：
 * <p>
 * additive -> multiplicative | multiplicative + additive
 * multiplicative -> primary | primary * multiplicative
 * <p>
 * 递归项在右边，会自然的对应右结合。我们真正需要的是左结合。
 */
public class SimpleCalculator {

    private static Logger LOG = LoggerFactory.getLogger(SimpleCalculator.class);

    private IMultiplicative multiplicative = new SimpleMultiplicative(new Primary());
    //    private IAdditive additive = new LeftCombinationAdditive(multiplicative);
    private IAdditive additive = new RightCombinationAdditive(multiplicative);


    public static void main(String[] args) throws Exception {
//        testDumpIntDeclare("int a = b+3;");
//        testEvaluate("2+3*4");
//        testEvaluate("2+3+4");
//        //测试异常语法
//        testEvaluate("2+");
//
//        testDumpAST("2+3+4+5");

        testParser("int age = 45+2; age= 20; age+10*2;");
//        //测试异常语法
//        testParser("2+3+;");
//        //测试异常语法
//        testParser("2+3*;");

    }

    private static void testParser(final String script) {
        SimpleParser parser = new SimpleParser();
        ASTNode tree = null;

        try {
            LOG.info("parse:{}", script);
            tree = parser.parse(script);
            parser.dumpAST(tree, "");
        } catch (Exception e) {
            LOG.error("testParser#Failed", e);
        }
    }

    private static void testDumpIntDeclare(String script) {
        SimpleCalculator calculator = new SimpleCalculator();
        LOG.info("DumpIntDeclare:{}==>{}", script, calculator.dumpIntDeclare(script));
    }

    private static void testEvaluate(String script) throws Exception {
        SimpleCalculator calculator = new SimpleCalculator();
        LOG.info("Evaluate:2+3*4={}", calculator.evaluate(script));
    }

    private static void testDumpAST(String script) throws Exception {
        SimpleCalculator calculator = new SimpleCalculator();
        LOG.info("DumpAST:{}==>", script);
    }


    /**
     * @param script
     * @return
     * @throws Exception
     */
    public boolean dumpIntDeclare(String script) {
        LOG.info("dumpIntDeclare#Running...{}", script);
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(script);
        boolean result;
        try {
            SimpleASTNode node = intDeclare(tokens);
            dumpAST(node, "");
            result = true;
        } catch (Exception e) {
            LOG.error("dumpIntDeclare#Failure", e);
            result = false;
        }
        LOG.info("dumpIntDeclare#Finish@{}", result);
        return result;
    }

    /**
     * 打印输出AST的树状结构。
     *
     * @param script
     */
    public void dumpAST(String script) throws Exception {
        try {
            ASTNode tree = parse(script);
            dumpAST(tree, "");
        } catch (Exception e) {
            LOG.error("dumpAST#Failure", e);
            throw e;
        }
    }

    /**
     * 执行脚本，并打印输出AST和求值过程。
     *
     * @param script
     */
    public int evaluate(String script) throws Exception {
        try {
            ASTNode tree = parse(script);

            dumpAST(tree, "");
            return evaluate(tree, "");
        } catch (Exception e) {
            LOG.error("evaluate#Failure", e);
            throw e;
        }
    }

    /**
     * 解析脚本，并返回根节点
     *
     * @param code
     * @return
     * @throws Exception
     */
    public ASTNode parse(String code) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(code);

        ASTNode rootNode = prog(tokens);

        return rootNode;
    }

    /**
     * 对某个AST节点求值，并打印求值过程。
     *
     * @param node
     * @param indent 打印输出时的缩进量，用tab控制
     * @return
     */
    private int evaluate(ASTNode node, String indent) {
        int result = 0;
        LOG.info("evaluate#" + indent + "Calculating: " + node.getType());
        switch (node.getType()) {
            case Programm:
                for (ASTNode child : node.getChildren()) {
                    result = evaluate(child, indent + "\t");
                }
                break;
            case Additive:
                ASTNode child1 = node.getChildren().get(0);
                int firstVal = evaluate(child1, indent + "\t");
                ASTNode child2 = node.getChildren().get(1);
                int secondVal = evaluate(child2, indent + "\t");
                if (node.getText().equals("+")) {
                    result = firstVal + secondVal;
                } else {
                    result = firstVal - secondVal;
                }
                break;
            case Multiplicative:
                child1 = node.getChildren().get(0);
                firstVal = evaluate(child1, indent + "\t");
                child2 = node.getChildren().get(1);
                secondVal = evaluate(child2, indent + "\t");
                if (node.getText().equals("*")) {
                    result = firstVal * secondVal;
                } else {
                    result = firstVal / secondVal;
                }
                break;
            case IntLiteral:
                result = Integer.valueOf(node.getText()).intValue();
                break;
            default:
        }
        LOG.info("evaluate#" + indent + "Result: " + result);
        return result;
    }

    /**
     * 语法解析：根节点
     * Programm Calculator
     * AdditiveExp +
     * IntLiteral 2
     * MulticativeExp *
     * IntLiteral 3
     * IntLiteral 5
     *
     * @return
     * @throws Exception
     */
    private SimpleASTNode prog(TokenReader tokens) throws Exception {
        SimpleASTNode node = new SimpleASTNode(ASTNodeType.Programm, "Calculator");
        SimpleASTNode child = additive.additive(tokens);

        if (null != child) {
            node.addChild(child);
        }
        return node;
    }

    /**
     * intDeclaration : Int Identifier ('=' additiveExpression)?;
     * 整型变量声明语句，如：
     * int a;
     * int b = 2*3;
     * <p>
     * // 伪代码
     * MatchIntDeclare(){
     * MatchToken(Int)；        // 匹配 Int 关键字
     * MatchIdentifier();       // 匹配标识符
     * MatchToken(equal);       // 匹配等号
     * MatchExpression();       // 匹配表达式
     * }
     *
     * @return
     * @throws Exception
     */
    private SimpleASTNode intDeclare(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        //预读
        Token token = tokens.peek();
        //匹配Int
        if (Token.matchToken(token, TokenType.Int)) {
            //消耗掉int
            token = tokens.read();
            //匹配标识符
            if (Token.matchNextToken(tokens, TokenType.Identifier)) {
                //消耗掉标识符
                token = tokens.read();
                //创建当前节点，并把变量名记到AST节点的文本值中，这里新建一个变量子节点也是可以的
                node = new SimpleASTNode(ASTNodeType.IntDeclaration, token.getText());
                //预读
                token = tokens.peek();
                if (Token.matchToken(token, TokenType.Assignment)) {
                    //消耗掉等号
                    tokens.read();
                    //匹配一个表达式
                    SimpleASTNode child = additive.additive(tokens);
                    if (null == child) {
                        throw new Exception("intDeclare#invalide variable initialization, expecting an expression");
                    } else {
                        node.addChild(child);
                    }
                }
            } else {
                throw new Exception("intDeclare#variable name expected");
            }

            if (null != node) {
                token = tokens.peek();
                if (Token.matchToken(token, TokenType.SemiColon)) {
                    tokens.read();
                } else {
                    throw new Exception("intDeclare#invalid statement, expecting semicolon");
                }
            }
        }


        return node;
    }

    /**
     * 打印输出AST的树状结构
     *
     * @param node
     * @param indent 缩进字符，由tab组成，每一级多一个tab
     */
    private void dumpAST(ASTNode node, String indent) {
        LOG.info("{} {} {}", indent, node.getType(), node.getText());
        for (ASTNode child : node.getChildren()) {
            dumpAST(child, indent + "\t");
        }
    }
}
