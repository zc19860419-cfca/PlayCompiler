package play.compiler.ast;

import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.TokenReader;

/**
 * @Author: zhangchong
 * @Description: 语法加法表达式接口
 */
public interface IAdditive {
    SimpleASTNode additive(TokenReader tokens) throws Exception;
}