package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class LinenoManger {
    private final CompilationUnit cu;

    public LinenoManger(CompilationUnit cu) {
        this.cu = cu;
    }

    public int getLineno(ASTNode node) {
        return cu.getLineNumber(node.getStartPosition()) - 1;
    }
}
