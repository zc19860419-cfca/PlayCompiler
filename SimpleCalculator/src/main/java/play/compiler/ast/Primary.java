package play.compiler.ast;

import play.compiler.ast.node.ASTNodeType;
import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.Token;
import play.compiler.lexer.TokenReader;
import play.compiler.lexer.TokenType;

/**
 * @Author: zhangchong
 * @Description: 基础表达式
 */
public class Primary {

    /**
     * 语法解析：基础表达式
     *
     * @param tokens
     * @param additive
     * @return
     * @throws Exception
     */
    public SimpleASTNode primary(TokenReader tokens, IAdditive additive) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if (null != token) {
            if (Token.matchToken(token, TokenType.IntLiteral)) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.IntLiteral, token.getText());
            } else if (Token.matchToken(token, TokenType.Identifier)) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.Identifier, token.getText());
            } else if (Token.matchToken(token, TokenType.LeftParen)) {
                //(additive)
                tokens.read();
                node = additive.additive(tokens);
                if (null != node) {
                    token = tokens.peek();
                    if (null != node && TokenType.RightParen == token.getType()) {
                        tokens.read();
                    } else {
                        throw new Exception("primary#expecting right parenthesis");
                    }
                } else {
                    throw new Exception("primary#expecting an additive expression inside parenthesis");
                }
            }
        }
        //这个方法也做了AST的简化，就是不用构造一个primary节点，直接返回子节点。因为它只有一个子节点。
        return node;
    }

}
