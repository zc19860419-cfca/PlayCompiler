package play.compiler.ast;

import play.compiler.ast.node.ASTNodeType;
import play.compiler.lexer.SimpleASTNode;
import play.compiler.lexer.Token;
import play.compiler.lexer.TokenReader;
import play.compiler.lexer.TokenType;

/**
 * @Author: zhangchong
 * @Description: 满足左结合的加法表达式语法分析
 */
public class LeftCombinationAdditive implements IAdditive {

    private final IMultiplicative multiplicative;

    public LeftCombinationAdditive(IMultiplicative multiplicativeAST) {
        this.multiplicative = multiplicativeAST;

    }

    /**
     * 语法解析：加法表达式(为解决左递归问题引入了右结合律问题的修改)
     * add ::= mul | add + mul
     * mul ::= pri | mul * pri
     * pri ::= Id | Num | (add)
     * <p>
     * 消除左递归，用一个标准的方法，就能够把左递归文法改写成非左递的文法
     * (把符号终结符提到开头)
     * add ::= mul add'  {add ::= mul (+ mul)*}
     * add' ::= + mul add' | ε
     *
     * @return
     * @throws Exception
     */
    public SimpleASTNode additive(TokenReader tokens) throws Exception {
        /**
         * 应用 add 规则
         */
        SimpleASTNode leftestChild = multiplicative.multiplicative(tokens, this);
        SimpleASTNode node = leftestChild;
        /**
         *   add ::= mul (+ mul)*
         *   对于 (+ mul)* 这部分，我们其实可以写成一个循环，而不是一次次的递归调用
         */
        if (null != leftestChild) {
            Token token;
            while (true) {
                token = tokens.peek();
                if (Token.matchToken(token, TokenType.Plus) || Token.matchToken(token, TokenType.Minus)) {
                    //消耗掉 +/- 运算符
                    token = tokens.read();
                    //计算下级节点
                    SimpleASTNode rightChild = multiplicative.multiplicative(tokens, this);
                    // 注意，新节点在顶层，保证正确的结合性
                    node = new SimpleASTNode(ASTNodeType.Additive, token.getText());
                    node.addChild(leftestChild);
                    node.addChild(rightChild);
                    leftestChild = node;
                } else {
                    break;
                }
            }

        }
        return node;
    }
}
