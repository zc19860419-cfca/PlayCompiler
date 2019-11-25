package play.compiler.script.compile;

import org.antlr.v4.runtime.ParserRuleContext;
import play.compiler.script.generate.PlayScriptBaseListener;
import play.compiler.script.generate.PlayScriptParser.*;

import java.util.Stack;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description 第一遍扫描, 识别出所有类型(包括类和函数), 以及Scope
 * 但是函数的参数信息要等到下一个阶段才会添加进去。
 * 把自定义类、函数和和作用域的树都分析出来。
 */
public class TypeAndScopeScanner extends PlayScriptBaseListener {
    private AnnotatedTree at = null;

    private Stack<Scope> scopeStack = new Stack<Scope>();

    public TypeAndScopeScanner(AnnotatedTree at) {
        this.at = at;
    }

    /**
     * @param ctx
     */
    @Override
    public void enterProg(ProgContext ctx) {
        NameSpace scope = new NameSpace("", currentScope(), ctx);
        at.setNameSpace(scope);
        pushScope(scope, ctx);
    }

    @Override
    public void exitProg(ProgContext ctx) {
        popScope();
    }

    /**
     * 进入块作用域
     * block
     * : '{' blockStatements '}'
     * ;
     * <p>
     * blockStatements
     * : blockStatement*
     * ;
     * <p>
     * blockStatement
     * : variableDeclarators ';'
     * | statement
     * | functionDeclaration (对于函数，不需要再额外建一个scope)
     * | classDeclaration
     * ;
     *
     * @param ctx
     */
    @Override
    public void enterBlock(BlockContext ctx) {
        if (!(ctx.parent instanceof FunctionBodyContext)) {
            BlockScope scope = new BlockScope(currentScope(), ctx);
            currentScope().addSymbol(scope);
            pushScope(scope, ctx);
        }
    }

    @Override
    public void exitBlock(BlockContext ctx) {
        if (!(ctx.parent instanceof FunctionBodyContext)) {
            popScope();
        }
    }


    /**
     * 进入 Statement 语法定义节点
     * statement
     * : blockLabel=block
     * | IF parExpression statement (ELSE statement)?
     * | FOR '(' forControl ')' statement           (为for建立额外的Scope)
     * | WHILE parExpression statement
     * | DO statement WHILE parExpression ';'
     * | SWITCH parExpression '{' switchBlockStatementGroup* switchLabel* '}'
     * | RETURN expression? ';'
     * | BREAK IDENTIFIER? ';'
     * | CONTINUE IDENTIFIER? ';'
     * | SEMI
     * | statementExpression=expression ';'
     * | identifierLabel=IDENTIFIER ':' statement
     * ;
     *
     * @param ctx
     */
    @Override
    public void enterStatement(StatementContext ctx) {
        if (ctx.FOR() != null) {
            BlockScope scope = new BlockScope(currentScope(), ctx);
            currentScope().addSymbol(scope);
            pushScope(scope, ctx);
        }
    }

    /**
     * 释放for语句的外层scope
     *
     * @param ctx
     */
    @Override
    public void exitStatement(StatementContext ctx) {
        if (ctx.FOR() != null) {
            popScope();
        }
    }

    /**
     * functionDeclaration
     * : typeTypeOrVoid? IDENTIFIER formalParameters ('[' ']')*
     * (THROWS qualifiedNameList)?
     * functionBody
     * ;
     * functionBody
     * : block
     * | ';'
     * ;
     *
     * @param ctx
     */
    @Override
    public void enterFunctionDeclaration(FunctionDeclarationContext ctx) {
        String idName = ctx.IDENTIFIER().getText();

        // 注意:目前funtion的信息并不完整，参数要等到TypeResolver.java中去确定。
        FunctionScope function = new FunctionScope(idName, currentScope(), ctx);

        at.types.add(function);

        currentScope().addSymbol(function);

        // 创建一个新的scope
        pushScope(function, ctx);
    }

    @Override
    public void exitFunctionDeclaration(FunctionDeclarationContext ctx) {
        popScope();
    }

    /**
     * classDeclaration
     * : CLASS IDENTIFIER
     * (EXTENDS typeType)?
     * (IMPLEMENTS typeList)?
     * classBody
     * ;
     * <p>
     * classBody
     * : '{' classBodyDeclaration* '}'
     * ;
     * <p>
     * classBodyDeclaration
     * : ';'
     * | memberDeclaration
     * ;
     * <p>
     * memberDeclaration
     * : functionDeclaration
     * | fieldDeclaration
     * | classDeclaration //内部类的语法结构
     * ;
     *
     * @param ctx
     */
    @Override
    public void enterClassDeclaration(ClassDeclarationContext ctx) {
        final String idName = ctx.IDENTIFIER().getText();

        ClassScope theClassScope = new ClassScope(idName, ctx);
        at.types.add(theClassScope);

        if (null != at.lookupClass(currentScope(), idName)) {
            // 只是报警，但仍然继续解析
            at.log("duplicate class name:" + idName, ctx);
        }

        currentScope().addSymbol(theClassScope);

        //创建一个新的scope
        pushScope(theClassScope, ctx);
    }

    @Override
    public void exitClassDeclaration(ClassDeclarationContext ctx) {
        popScope();
    }

    /**
     * 从栈中弹出scope
     */
    private void popScope() {
        scopeStack.pop();
    }

    private Scope pushScope(Scope scope, ParserRuleContext ctx) {
        at.node2Scope.put(ctx, scope);
        scope.ctx = ctx;
        scopeStack.push(scope);
        return scope;
    }

    /**
     * 在遍历树的过程中,当前的Scope
     *
     * @return
     */
    private Scope currentScope() {
        return scopeStack.size() > 0 ? scopeStack.peek() : null;
    }
}
