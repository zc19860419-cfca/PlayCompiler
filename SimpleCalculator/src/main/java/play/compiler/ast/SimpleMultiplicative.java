package play.compiler.ast;

import play.compiler.ast.node.ASTNodeType;
import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.Token;
import play.compiler.lexer.TokenReader;
import play.compiler.lexer.TokenType;

/**
 * @Author: zhangchong
 * @Description: 乘法表达式
 */
public class SimpleMultiplicative implements IMultiplicative {

    private final Primary primary;

    public SimpleMultiplicative(Primary primary) {
        this.primary = primary;

    }

    /**
     * 语法解析：乘法表达式
     *
     * @return
     * @throws Exception
     */
    @Override
    public SimpleASTNode multiplicative(TokenReader tokens, IAdditive additive) throws Exception {
        SimpleASTNode child1 = primary.primary(tokens, additive);
        SimpleASTNode node = child1;

        Token token = tokens.peek();
        if (null != child1 && null != token) {
            if (Token.matchToken(token, TokenType.Star) || Token.matchToken(token, TokenType.Slash)) {
                token = tokens.read();
                SimpleASTNode child2 = primary.primary(tokens, additive);
                if (null != child2) {
                    node = new SimpleASTNode(ASTNodeType.Multiplicative, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                } else {
                    throw new Exception("multiplicative#invalid expression, expecting the right part.");
                }
            }
        }
        return node;
    }
}
