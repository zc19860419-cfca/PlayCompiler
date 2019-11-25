package play.compiler.ast;

import play.compiler.ast.node.ASTNodeType;
import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.Token;
import play.compiler.lexer.TokenReader;
import play.compiler.lexer.TokenType;

/**
 * @Author: zhangchong
 * @Description: 满足右结合的加法表达式语法分析
 * 计算的结合性是有问题的。
 * <p>
 * additive -> multiplicative | multiplicative + additive
 * multiplicative -> primary | primary * multiplicative    //感谢@Void_seT，原来写成+号了，写错了。
 * <p>
 * 递归项在右边，会自然的对应右结合。我们真正需要的是左结合。
 */
public class RightCombinationAdditive implements IAdditive {

    private final IMultiplicative multiplicative;

    public RightCombinationAdditive(IMultiplicative multiplicativeAST) {
        this.multiplicative = multiplicativeAST;

    }

    @Override
    /**
     * 语法解析：加法表达式(为解决左递归问题引入了右结合律问题)
     * add ::= mul | add + mul {扩展巴克斯范式:add -> mul (+ mul)*}
     * mul ::= pri | mul * pri
     * pri ::= Id | Num | (add)
     * <p>
     * 2+3+4=>展开AST 如下图所示,形成右结合律
     * additive
     * ______________|______________
     * |             |             |
     * IntLiteral          +            additive
     * 2                       ________|________
     * |       |       |
     * IntLiteral   +    additive
     * 3               |
     * IntLiteral
     * 4
     *
     * @return
     * @throws Exception
     */
    public SimpleASTNode additive(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = multiplicative.multiplicative(tokens, this);
        SimpleASTNode node = child1;

        Token token = tokens.peek();
        if (null != child1 && null != token) {
            if (Token.matchToken(token, TokenType.Plus) || Token.matchToken(token, TokenType.Minus)) {
                token = tokens.read();
                SimpleASTNode child2 = additive(tokens);
                if (null != child2) {
                    node = new SimpleASTNode(ASTNodeType.Additive, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                } else {
                    throw new Exception("additive#invalid expression, expecting the right part.");
                }
            }
        }
        return node;
    }
}
