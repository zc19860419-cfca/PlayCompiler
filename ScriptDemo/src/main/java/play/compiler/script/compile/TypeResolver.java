package play.compiler.script.compile;

import play.compiler.script.generate.PlayScriptBaseListener;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorIdContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorsContext;
import play.compiler.script.runtime.Type;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description 第二遍扫描。把变量、类继承、函数声明的类型都解析出来。也就是所有用到typeType的地方。
 * 做了自上而下的类型推导，也就是 I 属性的计算，包括变量 - 声明、类的继承声明、函数声明
 */
public class TypeResolver extends PlayScriptBaseListener {
    private AnnotatedTree at = null;

    public TypeResolver(AnnotatedTree at) {
        this.at = at;
    }

    /**
     * 把所有的变量声明加入符号表
     * variableDeclarator
     * : variableDeclaratorId ('=' variableInitializer)?
     * ;
     * variableDeclaratorId
     * : IDENTIFIER ('[' ']')*
     * ;
     * typeType
     *     : (classOrInterfaceType| functionType | primitiveType) ('[' ']')*
     *     ;
     *
     * IDENTIFIER:         Letter LetterOrDigit*;
     *
     * @param ctx
     */
    @Override
    public void enterVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        final String idName = ctx.IDENTIFIER().getText();
        //取得当前节点所属的作用域
        Scope scope = at.enclosingScopeOfNode(ctx);
        Variable variable = new Variable(idName, scope, ctx);

        //变量查重
        if (null != Scope.getVariable(scope, idName)) {
            at.log("Variable or parameter already Declared: " + idName, ctx);
        }

        scope.addSymbol(variable);
        at.symbolOfNode.put(ctx, variable);
    }

    /**
     * <p>
     * int              a
     * variableDeclarators[到这一级就可以获取定义的变量类型了]
     * typeType         variableDeclarator
     * primitiveType    variableDeclaratorId
     * </p>
     * <p>
     * 设置所声明的变量的类型
     *
     * @param ctx
     */
    @Override
    public void exitVariableDeclarators(VariableDeclaratorsContext ctx) {
        // 设置变量类型(首先从Antlr中取出节点 "类型的类型")
        Type type = at.typeOfNode.get(ctx.typeType());

        for (VariableDeclaratorContext child : ctx.variableDeclarator()) {
            //child.variableDeclaratorId() 相当于取出 a
            Variable variable = (Variable) at.symbolOfNode.get(child.variableDeclaratorId());
            variable.type = type;
        }
    }
}
