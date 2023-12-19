package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import pascal.taie.frontend.newfrontend.FrontendException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImportExtractor extends ASTVisitor {

    Set<String> names = new HashSet<>();

    @Override
    public boolean visit(QualifiedType node) {
        ITypeBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        ITypeBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        ITypeBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }


    @Override
    public boolean visit(ImportDeclaration node) {
        IBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    private void extractBinding(IBinding binding) {
        if (binding == null) {
            throw new FrontendException(
                    "Binding resolving failure, following the JDT error message to fix it");
        }
        if (binding instanceof ITypeBinding typeBinding) {
            String binaryTypeName;
            if (typeBinding.isArray()) {
                binaryTypeName = typeBinding.getElementType().getBinaryName();
            } else {
                binaryTypeName = typeBinding.getBinaryName();
            }
            names.add(binaryTypeName);
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
