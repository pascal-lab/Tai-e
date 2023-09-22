package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImportExtractor extends ASTVisitor {

    Set<String> names = new HashSet<>();

    @Override
    public boolean visit(ImportDeclaration node) {
        IBinding binding = node.resolveBinding();
        assert binding != null;
        extractBinding(binding);
        return false;
    }

    private void extractBinding(IBinding binding) {
        if (binding instanceof ITypeBinding typeBinding) {
            names.add(typeBinding.getBinaryName());
        } else if (binding instanceof IMethodBinding methodBinding) {
            names.add(methodBinding.getDeclaringClass().getBinaryName());
        }
        // TODO: 1. handle Annotation (IMemberValuePairBinding)
        //       2. handle wildcard import (IPackageBinding)
    }

    public List<String> getDependencies() {
        return names.stream().toList();
    }
}
