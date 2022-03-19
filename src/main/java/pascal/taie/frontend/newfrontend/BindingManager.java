package pascal.taie.frontend.newfrontend;


import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class BindingManager {
    private final Map<Expression, Type> innerMap;
    private final Map<ASTNode, ASTNode> copyMap;

    public BindingManager() {
        innerMap = Maps.newMap();
        copyMap  = Maps.newMap();
    }

    public Type getTypeOfExp(Expression exp) {
        ITypeBinding t = exp.resolveTypeBinding();
        if (t != null) {
            return TypeUtils.JDTTypeToTaieType(t);
        } else {
            Type type = innerMap.get(exp);
            if (type == null) {
                throw new NewFrontendException("Type of " + exp + " can't be resolved");
            }
            return type;
        }
    }

    public void setType(Expression exp, Type t) {
        innerMap.put(exp, t);
    }

    public ASTNode copyNode(AST ast, ASTNode node) {
        var newNode = ASTNode.copySubtree(ast, node);
        copyMap.put(newNode, node);
        return newNode;
    }

    public ASTNode getRealNode(ASTNode node) {
        return copyMap.getOrDefault(node, node);
    }
}
