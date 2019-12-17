package play.compiler.lexer;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.utils.StringUtils;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangchong
 * @Description: 一个简单的词法分析器实现。
 */
public class SimpleLexer {
    private static Logger LOG = LoggerFactory.getLogger(SimpleLexer.class);
    /**
     * 保存解析出来的Token
     */
    private List<Token> tokens;
    /**
     * 临时保存token的文本
     */
    private StringBuffer tokenText;

    /**
     * 当前正在解析的Token
     */
    private SimpleToken currentToken = null;

    /**
     * 解析字符串，形成Token。
     * 这是一个有限状态自动机，在不同的状态中迁移。
     *
     * @param code
     * @return
     */
    public TokenReader tokenize(String code) {
        tokens = new ArrayList<Token>();
        CharArrayReader reader = new CharArrayReader(code.toCharArray());
        tokenText = new StringBuffer(1024);
        currentToken = new SimpleToken();
        int currentCharValue = 0;
        char currentChar = 0;
        DFAState state = DFAState.Initial;
        try {
            while (-1 != (currentCharValue = reader.read())) {
                currentChar = (char) currentCharValue;
                switch (state) {
                    case Initial:
                        state = initToken(currentChar);
                        break;
                    case Id:
                        if (StringUtils.isDigit(currentChar) || StringUtils.isAlpha(currentChar)) {
                            tokenText.append(currentChar);
                        } else {
                            state = initToken(currentChar);
                        }
                        break;
                    case GT:
                        if (currentChar == '=') {
                            currentToken.type = TokenType.GE;
                            state = DFAState.GE;
                            tokenText.append(currentChar);
                        } else {
                            //退出GT状态,并保存Token
                            state = initToken(currentChar);
                        }
                        break;

                    case GE:
                    case Assignment:
                    case Plus:
                    case Minus:
                    case Star:
                    case Slash:
                    case SemiColon:
                    case LeftParen:
                    case RightParen:
                        state = initToken(currentChar);
                        break;
                    case IntLiteral:
                        if (StringUtils.isDigit(currentChar)) {
                            tokenText.append(currentChar);
                        } else {
                            state = initToken(currentChar);
                        }
                        break;
                    case Id_int1:
                        if ('n' == currentChar) {
                            state = DFAState.Id_int2;
                            tokenText.append(currentChar);
                        } else if (StringUtils.isDigit(currentChar) || StringUtils.isAlpha(currentChar)) {
                            state = DFAState.Id;
                            tokenText.append(currentChar);
                        } else {
                            state = initToken(currentChar);
                        }
                        break;
                    case Id_int2:
                        if ('t' == currentChar) {
                            state = DFAState.Id_int3;
                            tokenText.append(currentChar);
                        } else if (StringUtils.isDigit(currentChar) || StringUtils.isAlpha(currentChar)) {
                            state = DFAState.Id;
                            tokenText.append(currentChar);
                        } else {
                            state = initToken(currentChar);
                        }
                        break;
                    case Id_int3:
                        if (StringUtils.isBlank(currentChar)) {
                            currentToken.type = TokenType.Int;
                            state = initToken(currentChar);
                        } else {
                            state = DFAState.Id;
                            tokenText.append(currentChar);
                        }
                        break;

                    default:
                }
            }

            if (tokenText.length() > 0) {
                initToken(currentChar);
            }
        } catch (IOException e) {
            LOG.error("tokenize#Failure", e);
        }
        return new SimpleTokenReader(tokens);
    }

    /**
     * 打印所有的Token
     *
     * @param tokenReader
     */
    public static void dump(SimpleTokenReader tokenReader) {
        LOG.info("text\ttype");
        Token token = null;
        while ((token = tokenReader.read()) != null) {
            LOG.info("{}\t\t{}", token.getText(), token.getType());
        }
    }

    /**
     * 有限状态机进入初始状态。
     * 这个初始状态其实并不做停留，它马上进入其他状态。
     * 开始解析的时候，进入初始状态；某个Token解析完毕，也进入初始状态，
     * 在这里把Token记下来，然后建立一个新的Token。
     *
     * @param ch
     * @return
     */
    private DFAState initToken(char ch) {
        if (!StringUtils.isEmpty(tokenText)) {
            currentToken.text = tokenText.toString();
            tokens.add(currentToken);

            tokenText = new StringBuffer(1024);
            currentToken = new SimpleToken();
        }

        DFAState newState = DFAState.Initial;
        if (StringUtils.isAlpha(ch)) {
            if (ch == 'i') {
                newState = DFAState.Id_int1;
            } else {
                newState = DFAState.Id;
            }
            currentToken.type = TokenType.Identifier;
            tokenText.append(ch);
        } else if (StringUtils.isDigit(ch)) {
            //整型字面量
            newState = DFAState.IntLiteral;
            currentToken.type = TokenType.IntLiteral;
            tokenText.append(ch);
        } else if (ch == '>') {
            newState = DFAState.GT;
            currentToken.type = TokenType.GT;
            tokenText.append(ch);
        } else if (ch == '+') {
            newState = DFAState.Plus;
            currentToken.type = TokenType.Plus;
            tokenText.append(ch);
        } else if (ch == '-') {
            newState = DFAState.Minus;
            currentToken.type = TokenType.Minus;
            tokenText.append(ch);
        } else if (ch == '*') {
            newState = DFAState.Star;
            currentToken.type = TokenType.Star;
            tokenText.append(ch);
        } else if (ch == '/') {
            newState = DFAState.Slash;
            currentToken.type = TokenType.Slash;
            tokenText.append(ch);
        } else if (ch == ';') {
            newState = DFAState.SemiColon;
            currentToken.type = TokenType.SemiColon;
            tokenText.append(ch);
        } else if (ch == '(') {
            newState = DFAState.LeftParen;
            currentToken.type = TokenType.LeftParen;
            tokenText.append(ch);
        } else if (ch == ')') {
            newState = DFAState.RightParen;
            currentToken.type = TokenType.RightParen;
            tokenText.append(ch);
        } else if (ch == '=') {
            newState = DFAState.Assignment;
            currentToken.type = TokenType.Assignment;
            tokenText.append(ch);
        } else {
            // skip all unknown patterns
            newState = DFAState.Initial;
        }
        return newState;
    }

    /**
     * Token的一个简单实现。只有类型和文本值两个属性。
     */
    private final class SimpleToken implements Token {
        //Token类型
        private TokenType type = null;

        //文本值
        private String text = null;


        @Override
        public TokenType getType() {
            return type;
        }

        @Override
        public String getText() {
            return text;
        }
    }

    /**
     * 有限状态机的各种状态。
     */
    private enum DFAState {
        Initial,

        /**
         * 逐字节读取,所以需要这么多状态
         */
        If, Id_if1, Id_if2, Else, Id_else1, Id_else2, Id_else3, Id_else4, Int, Id_int1, Id_int2, Id_int3, Id, GT, GE,

        Assignment,

        Plus, Minus, Star, Slash,

        SemiColon,
        LeftParen,
        RightParen,

        IntLiteral
    }

    /**
     * 一个简单的Token流。是把一个Token列表进行了封装。
     */
    private class SimpleTokenReader implements TokenReader {
        List<Token> tokens = null;
        int pos = 0;

        public SimpleTokenReader(List<Token> tokens) {
            this.tokens = tokens;
        }

        @Override
        public Token read() {
            Token readed = null;
            if (pos < tokens.size()) {
                readed = tokens.get(pos++);
            }
            return readed;
        }

        @Override
        public Token peek() {
            Token peeked = null;
            if (pos < tokens.size()) {
                peeked = tokens.get(pos);
            }
            return peeked;
        }

        @Override
        public void unread() {
            if (pos > 0) {
                pos--;
            }
        }

        @Override
        public int getPosition() {
            return pos;
        }

        @Override
        public void setPosition(int position) {
            if (position >= 0 && position < tokens.size()) {
                pos = position;
            }
        }

    }
}
