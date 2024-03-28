package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import pascal.taie.frontend.newfrontend.java.ClassExtractor;
import pascal.taie.frontend.newfrontend.java.JDTStringReps;
import pascal.taie.frontend.newfrontend.java.JavaClassInit;
import pascal.taie.frontend.newfrontend.java.JavaInit;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JavaSource implements ClassSource {

    private final CompilationUnit unit;

    private final String binaryName;

    private final List<JavaInit> instanceInits;

    private final List<JavaClassInit> classInits;

    public ASTNode getTypeDeclaration() {
        return typeDeclaration;
    }

    private final ASTNode typeDeclaration;

    public JavaSource(CompilationUnit unit, ASTNode typeDeclaration) {
        this.unit = unit;
        this.typeDeclaration = typeDeclaration;
        ITypeBinding binding = ClassExtractor.getBinding(typeDeclaration);
        binaryName = JDTStringReps.getBinaryName(binding);
        instanceInits = new ArrayList<>();
        classInits = new ArrayList<>();
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

    public void addNewCinit(JavaClassInit classInit) {
        classInits.add(classInit);
    }

    public List<JavaClassInit> getClassInits() {
        return classInits;
    }
}
