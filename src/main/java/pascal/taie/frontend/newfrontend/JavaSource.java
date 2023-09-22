package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.CompilationUnit;
import pascal.taie.frontend.newfrontend.java.BinaryNameExtractor;
import pascal.taie.frontend.newfrontend.java.JavaInit;

import java.util.ArrayList;
import java.util.List;

public class JavaSource implements ClassSource {

    private final CompilationUnit unit;

    private final String binaryName;

    private final List<JavaInit> instanceInits;

    public JavaSource(CompilationUnit unit) {
        this.unit = unit;
        binaryName = BinaryNameExtractor.getBinaryNameFromCompilationUnit(unit);
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
