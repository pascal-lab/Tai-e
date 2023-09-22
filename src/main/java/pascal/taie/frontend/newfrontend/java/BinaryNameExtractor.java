package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class BinaryNameExtractor extends ASTVisitor {
    private String binaryName;

    public String getBinaryName() {
        return binaryName;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        // TODO: check if name equal filename (maybe multiple class in a .java file)
        if (node.isPackageMemberTypeDeclaration()) {
            binaryName = node.resolveBinding().getBinaryName();
        }
        return false;
    }

    public static String getBinaryNameFromCompilationUnit(CompilationUnit compilationUnit) {
        BinaryNameExtractor extractor = new BinaryNameExtractor();
        compilationUnit.accept(extractor);
        String name = extractor.getBinaryName();
        assert name != null;
        return name;
    }
}
