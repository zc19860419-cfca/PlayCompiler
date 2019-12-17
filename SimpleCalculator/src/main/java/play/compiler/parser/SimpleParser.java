package play.compiler.parser;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.ast.IAdditive;
import play.compiler.ast.IMultiplicative;
import play.compiler.ast.Primary;
import play.compiler.ast.RightCombinationAdditive;
import play.compiler.ast.SimpleMultiplicative;
import play.compiler.ast.node.ASTNode;
import play.compiler.ast.node.ASTNodeType;
import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.SimpleLexer;
import play.compiler.lexer.Token;
import play.compiler.lexer.TokenReader;
import play.compiler.lexer.TokenType;

/**
 * @Author: zhangchong
 * @Description:
 */
public class SimpleParser {

    private static Logger LOG = LoggerFactory.getLogger(SimpleParser.class);

    private IMultiplicative multiplicative = new SimpleMultiplicative(new Primary());
    private IAdditive additive = new RightCombinationAdditive(multiplicative);

    /**
     * 解析脚本
     *
     * @param script
     * @return
     * @throws Exception
     */
    public ASTNode parse(String script) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(script);
        ASTNode rootNode = prog(tokens);
        return rootNode;
    }

    /**
     * AST的根节点，解析的入口。
     * statement
     * : intDeclaration
     * | expressionStatement
     * | assignmentStatement
     * ;
     *
     * @return
     * @throws Exception
     */
    private ASTNode prog(TokenReader tokens) throws Exception {
        final SimpleASTNode node = new SimpleASTNode(ASTNodeType.Programm, "pwc");
        while (null != tokens.peek()) {
            SimpleASTNode child = intDeclare(tokens);

            if (child == null) {
                child = expressionStatement(tokens);
            }

            if (child == null) {
                child = assignmentStatement(tokens);
            }

            if (child != null) {
                node.addChild(child);
            } else {
                throw new Exception("unknown statement");
            }
        }
        return node;
    }

    /**
     * 整型变量声明，如：
     * int a;
     * int b = 2*3;
     *
     * @return
     * @throws Exception
     */
    private SimpleASTNode intDeclare(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if (Token.matchToken(token, TokenType.Int)) {
            tokens.read();
            token = tokens.peek();
            if (Token.matchToken(token, TokenType.Identifier)) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.IntDeclaration, token.getText());
                if (Token.matchNextToken(tokens, TokenType.Assignment)) {
                    //取出等号
                    tokens.read();
                    SimpleASTNode child = additive.additive(tokens);
                    if (null == child) {
                        throw new Exception("invalide variable initialization, expecting an expression");
                    } else {
                        node.addChild(child);
                    }
                }
            } else {
                throw new Exception("variable name expected");
            }

            if (null != node) {
                if (Token.matchNextToken(tokens, TokenType.SemiColon)) {
                    tokens.read();
                } else {
                    throw new Exception("invalid statement, expecting semicolon");
                }
            }
        }
        return node;
    }

    /**
     * 表达式语句，即表达式后面跟个分号。
     *
     * @return
     * @throws Exception
     */
    private SimpleASTNode expressionStatement(TokenReader tokens) throws Exception {
        int pos = tokens.getPosition();
        SimpleASTNode node = additive.additive(tokens);
        if (null != node) {
            try {
                if (Token.matchNextToken(tokens, TokenType.SemiColon)) {
                    tokens.read();
                } else {
                    throw new IllegalStateException("Illegal expression not end with ';'");
                }

            } catch (IllegalStateException e) {
                //回溯
                node = null;
                tokens.setPosition(pos);
            }
        }
        return node;
    }

    /**
     * 赋值语句，如age = 10*2;
     *
     * @return
     * @throws Exception
     */
    private SimpleASTNode assignmentStatement(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        //预读，看看下面是不是标识符
        Token token = tokens.peek();
        if (Token.matchToken(token, TokenType.Identifier)) {
            //读入标识符
            token = tokens.read();
            node = new SimpleASTNode(ASTNodeType.AssignmentStmt, token.getText());
            //预读，看看下面是不是等号
            if (Token.matchNextToken(tokens, TokenType.Assignment)) {
                //Case1:是等号
                //消耗掉等号
                tokens.read();
                SimpleASTNode rightChild = additive.additive(tokens);
                //出错，等号右面没有一个合法的表达式
                if (null == rightChild) {
                    throw new Exception("invalide assignment statement, expecting an expression");
                } else {
                    node.addChild(rightChild);
                    if (Token.matchNextToken(tokens, TokenType.SemiColon)) {
                        //消耗掉这个分号
                        tokens.read();
                    } else {
                        //报错，缺少分号
                        throw new Exception("invalid statement, expecting semicolon");
                    }
                }
            } else {
                //Case2:不是等号
                //回溯，吐出之前读入的标识符
                tokens.unread();
                node = null;
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
    public void dumpAST(ASTNode node, String indent) {
        LOG.info(indent + node.getType() + " " + node.getText());
        for (ASTNode child : node.getChildren()) {
            dumpAST(child, indent + "\t");
        }
    }
}
