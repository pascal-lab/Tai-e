package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import pascal.taie.ir.exp.MethodHandle;

import java.util.List;
import java.util.Optional;

/**
 * Locate a method by signature in a class file({@code CompilationUnit})
 */
public class MethodLocator {
    MethodDeclaration methodDeclaration;
    MethodLocator() {}
    Optional<MethodDeclaration> getMethodBySig(
            String className,
            String signature, CompilationUnit cu) {
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                // if class name is not equal, visit child to check if target is inner class
                var binName = node.getName().resolveTypeBinding().getBinaryName();
                if (! binName.equals(className)) {
                    return true;
                }

                // if class name is equal, visit all methods to locate.
                // DO NOT visit child after this method
                for (var i : node.getMethods()) {
                    var visitor = new LocateVisitor(signature);
                    i.accept(visitor);
                    if (visitor.res != null) {
                        methodDeclaration = visitor.res;
                        break;
                    }
                }
                return false;
            }
        });
        return Optional.ofNullable(methodDeclaration);
    }
}

class LocateVisitor extends ASTVisitor {
    private final String signature;
    public MethodDeclaration res;

    public LocateVisitor(String signature) {
        this.signature = signature;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if(TypeUtils.isSubSignature(signature, node.resolveBinding())) {
            res = node;
        }
        return false;
    }
}