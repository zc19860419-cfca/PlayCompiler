package play.compiler.ast;

import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.TokenReader;

/**
 * @Author: zhangchong
 * @Description:
 */
public interface IMultiplicative {
    SimpleASTNode multiplicative(TokenReader tokens, IAdditive refreshAdditiveAST) throws Exception;
}
