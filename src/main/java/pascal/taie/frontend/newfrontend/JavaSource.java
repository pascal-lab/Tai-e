package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import pascal.taie.frontend.newfrontend.java.ClassExtractor;
import pascal.taie.frontend.newfrontend.java.JDTStringReps;
import pascal.taie.frontend.newfrontend.java.JavaInit;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JavaSource implements ClassSource {

    private final CompilationUnit unit;

    private final String binaryName;

    private final List<JavaInit> instanceInits;

    public ASTNode getTypeDeclaration() {
        return typeDeclaration;
    }

    private final ASTNode typeDeclaration;

    public @Nullable String getOuterClass() {
        return outerClass;
    }

    private final String outerClass;

    public JavaSource(CompilationUnit unit, ASTNode typeDeclaration, String outerClass) {
        this.unit = unit;
        this.typeDeclaration = typeDeclaration;
        this.outerClass = outerClass;
        binaryName = JDTStringReps.getBinaryName(ClassExtractor.getBinding(typeDeclaration));
        instanceInits = new ArrayList<>();
    }

    @Override
    public String getClassName() {
        return binaryName;
    }

    @Override
    public boolean isApplication() {
        return true;
    }

    public CompilationUnit getUnit() {
        return unit;
    }

    public void addNewInit(JavaInit init) {
        this.instanceInits.add(init);
    }

    public List<JavaInit> getInstanceInits() {
        return instanceInits;
    }
}
