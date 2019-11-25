package play.compiler.ast.node;

import java.util.List;

/**
 * @Author: zhangchong
 * @Description:
 */
public interface ASTNode {
    ASTNode getParent();

    List<ASTNode> getChildren();

    ASTNodeType getType();

    String getText();
}
