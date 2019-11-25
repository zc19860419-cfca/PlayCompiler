package play.compiler.lexer;

/**
 * @Author: zhangchong
 * @Description: 词法单元接口
 */
public interface Token {
    TokenType getType();

    String getText();

    static boolean matchNextToken(TokenReader tokens, TokenType expected) {
        Token next = tokens.peek();
        return matchToken(next, expected);
    }

    static boolean matchToken(Token token, TokenType expected) {
        return token != null && expected == token.getType();
    }
}
