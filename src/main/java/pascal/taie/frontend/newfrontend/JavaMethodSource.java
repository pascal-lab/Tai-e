package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public record JavaMethodSource(CompilationUnit cu, ASTNode decl, JavaSource source) {}
