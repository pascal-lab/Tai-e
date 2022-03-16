package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Optional;

import static pascal.taie.frontend.newfrontend.JDTStringReps.getBinaryName;

/**
 * Locate a method by signature in a class file({@code CompilationUnit})
 */
public class MethodLocator {
    MethodDeclaration methodDeclaration;
    Logger logger = LogManager.getLogger(MethodLocator.class);
    Optional<MethodDeclaration> getMethodBySig(
            String className,
            String signature, CompilationUnit cu) {
        LocateVisitor lv = new LocateVisitor(signature);
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                // if class name is not equal, visit child to check if target is inner class
                var binName = getBinaryName(node.resolveBinding());
                logger.info("1: " + binName + " 2: "  + node.resolveBinding().getBinaryName() + " " + node.isLocalTypeDeclaration());
                if (! binName.equals(className)) {
                    return true;
                }

                // if class name is equal, visit all methods to locate.
                // DO NOT visit child after this method
                for (var i : node.getMethods()) {
                    i.accept(lv);
                    if (lv.res != null) {
                        methodDeclaration = lv.res;
                        break;
                    }
                }
                return false;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration acd) {
                var binName = getBinaryName(acd.resolveBinding());
                if (! binName.equals(className)) {
                    return true;
                }
                for (var i : acd.bodyDeclarations()) {
                    BodyDeclaration bd = (BodyDeclaration) i;
                    bd.accept(lv);
                    if (lv.success()) {
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

    public boolean success() {
        return ! (res == null);
    }
}