package play.compiler.script.compile;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.script.generate.PlayScriptBaseListener;
import play.compiler.script.generate.PlayScriptParser.ClassDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.ClassOrInterfaceTypeContext;
import play.compiler.script.generate.PlayScriptParser.FormalParameterContext;
import play.compiler.script.generate.PlayScriptParser.FunctionDeclarationContext;
import play.compiler.script.generate.PlayScriptParser.FunctionTypeContext;
import play.compiler.script.generate.PlayScriptParser.PrimitiveTypeContext;
import play.compiler.script.generate.PlayScriptParser.TypeListContext;
import play.compiler.script.generate.PlayScriptParser.TypeTypeContext;
import play.compiler.script.generate.PlayScriptParser.TypeTypeOrVoidContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorIdContext;
import play.compiler.script.generate.PlayScriptParser.VariableDeclaratorsContext;
import play.compiler.script.runtime.DefaultFunctionType;
import play.compiler.script.runtime.PrimitiveType;
import play.compiler.script.runtime.Type;
import play.compiler.script.runtime.VoidType;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description 第二遍扫描.
 * 把变量,类继承,函数声明的类型都解析出来,也就是所有用到 typeType 的地方.
 * 做了自上而下的类型推导,也就是 I 属性(继承属性)的计算,包括变量 - 声明,类的继承声明,函数声明
 */
public class TypeResolver extends PlayScriptBaseListener {
    private static Logger LOG = LoggerFactory.getLogger(TypeResolver.class);
    private AnnotatedTree at = null;

    public TypeResolver(AnnotatedTree at) {
        this.at = at;
    }

    /**
     * variableDeclarators
     * : typeType variableDeclarator (',' variableDeclarator)*
     * ;
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
            //对变量的类型进行赋值
            variable.type = type;
        }
    }

    /**
     * 把所有的变量声明加入符号表
     * <p>
     * variableDeclaratorId
     * : IDENTIFIER ('[' ']')*
     * ;
     * IDENTIFIER:         Letter LetterOrDigit*;
     *
     * @param ctx
     */
    @Override
    public void enterVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        final String idName = ctx.IDENTIFIER().getText();
        //取得当前节点所属的作用域,这是在第一遍扫描中将所作用域准备完毕的
        //为什么要放在第二遍,就是这里需要标记变量隶属于哪个作用域,而作用域是第一遍扫描得到的
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
     * 设置函数的返回值类型
     * functionDeclaration
     * : typeTypeOrVoid? IDENTIFIER formalParameters ('[' ']')*
     * (THROWS qualifiedNameList)?
     * functionBody
     * ;
     *
     * @param ctx
     */
    @Override
    public void exitFunctionDeclaration(FunctionDeclarationContext ctx) {
        LOG.info("exitFunctionDeclaration#Running...{}", ctx.IDENTIFIER().getText());
        //在第一遍扫描时 enterFunctionDeclaration 加入了所需要的函数对应的scope
        final FunctionScope function = (FunctionScope) at.node2Scope.get(ctx);
        if (null != ctx.typeTypeOrVoid()) {
            function.returnType = at.typeOfNode.get(ctx.typeTypeOrVoid());
        } else {
            //TODO 如果是类的构建函数，返回值应该是一个类吧？
        }

        //函数查重,检查名称和参数(这个时候参数已经齐了)
        Scope scope = at.enclosingScopeOfNode(ctx);
        FunctionScope found = Scope.getFunction(scope, function.name, function.getParamTypes());
        if (found != null && found != function) {
            at.log("Function or method already Declared: " + ctx.getText(), ctx);
        }

    }

    /**
     * 设置函数的参数的类型,这些参数已经在 enterVariableDeclaratorId 中作为变量声明了,现在设置它们的类型
     * formalParameters
     * : '(' formalParameterList? ')'
     * ;
     * <p>
     * formalParameterList
     * : formalParameter (',' formalParameter)* (',' lastFormalParameter)?
     * | lastFormalParameter
     * ;
     * <p>
     * formalParameter
     * : variableModifier* typeType variableDeclaratorId
     * ;
     * <p>
     * lastFormalParameter
     * : variableModifier* typeType '...' variableDeclaratorId
     * ;
     * <p>
     * variableModifier
     * : FINAL
     * //| annotation
     * ;
     *
     * @param ctx
     */
    @Override
    public void exitFormalParameter(FormalParameterContext ctx) {
        // 设置参数类型
        Type type = at.typeOfNode.get(ctx.typeType());
        Variable variable = (Variable) at.symbolOfNode.get(ctx.variableDeclaratorId());
        variable.type = (Type) type;

        // 添加到函数的参数列表里
        Scope scope = at.enclosingScopeOfNode(ctx);
        if (scope instanceof FunctionScope) {
            //TODO 从目前的语法来看，只有function才会使用FormalParameter
            ((FunctionScope) scope).parameters.add(variable);
        }
    }

    /**
     * 设置类的父类
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
        ClassScopeAndType theClass = (ClassScopeAndType) at.node2Scope.get(ctx);

        //设置父类
        if (ctx.EXTENDS() != null) {
            String parentClassName = ctx.typeType().getText();
            Type type = at.lookupType(parentClassName);
            if (type != null && type instanceof ClassScopeAndType) {
                //建立继承关系
                theClass.setParentClass((ClassScopeAndType) type);
            } else {
                at.log("unknown class: " + parentClassName, ctx);
            }
        }

    }

    /**
     * typeTypeOrVoid
     * : typeType
     * | VOID
     * ;
     * typeType
     * : (classOrInterfaceType| functionType | primitiveType) ('[' ']')*
     * ;
     * functionType
     * : FUNCTION typeTypeOrVoid '(' typeList? ')'
     * ;
     * 离开后才能解析出类型
     *
     * @param ctx
     */
    @Override
    public void exitTypeTypeOrVoid(TypeTypeOrVoidContext ctx) {
        if (ctx.VOID() != null) {
            at.typeOfNode.put(ctx, VoidType.instance());
        } else if (ctx.typeType() != null) {
            at.typeOfNode.put(ctx, (Type) at.typeOfNode.get(ctx.typeType()));
        }
    }

    /**
     * 冒泡向上，将下级的属性标注在本级
     * typeType
     * : (classOrInterfaceType| functionType | primitiveType) ('[' ']')*
     * ;
     *
     * @param ctx
     */
    @Override
    public void exitTypeType(TypeTypeContext ctx) {
        if (ctx.classOrInterfaceType() != null) {
            Type type = at.typeOfNode.get(ctx.classOrInterfaceType());
            at.typeOfNode.put(ctx, type);
        } else if (ctx.functionType() != null) {
            Type type = at.typeOfNode.get(ctx.functionType());
            at.typeOfNode.put(ctx, type);
        } else if (ctx.primitiveType() != null) {
            Type type = at.typeOfNode.get(ctx.primitiveType());
            at.typeOfNode.put(ctx, type);
        }

    }

    @Override
    public void enterClassOrInterfaceType(ClassOrInterfaceTypeContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            Scope scope = at.enclosingScopeOfNode(ctx);
            String idName = ctx.getText();
            ClassScopeAndType theClass = at.lookupClass(scope, idName);
            at.typeOfNode.put(ctx, theClass);
        }
    }

    /**
     * functionType
     * : FUNCTION typeTypeOrVoid '(' typeList? ')'
     * ;
     *
     * @param ctx
     */
    @Override
    public void exitFunctionType(FunctionTypeContext ctx) {
        DefaultFunctionType functionType = new DefaultFunctionType();
        at.types.add(functionType);

        at.typeOfNode.put(ctx, functionType);

        functionType.returnType = at.typeOfNode.get(ctx.typeTypeOrVoid());

        // 参数的类型
        if (ctx.typeList() != null) {
            TypeListContext tcl = ctx.typeList();
            for (TypeTypeContext ttc : tcl.typeType()) {
                Type type = at.typeOfNode.get(ttc);
                functionType.paramTypes.add(type);
            }
        }
    }

    @Override
    public void exitPrimitiveType(PrimitiveTypeContext ctx) {
        Type type = null;
        if (ctx.BOOLEAN() != null) {
            type = PrimitiveType.Boolean;
        } else if (ctx.INT() != null) {
            type = PrimitiveType.Integer;
        } else if (ctx.LONG() != null) {
            type = PrimitiveType.Long;
        } else if (ctx.FLOAT() != null) {
            type = PrimitiveType.Float;
        } else if (ctx.DOUBLE() != null) {
            type = PrimitiveType.Double;
        } else if (ctx.BYTE() != null) {
            type = PrimitiveType.Byte;
        } else if (ctx.SHORT() != null) {
            type = PrimitiveType.Short;
        } else if (ctx.CHAR() != null) {
            type = PrimitiveType.Char;
        } else if (ctx.STRING() != null) {
            type = PrimitiveType.String;
        }

        at.typeOfNode.put(ctx, type);
    }
}
