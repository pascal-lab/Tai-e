package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public record JavaMethodSource(CompilationUnit cu, MethodDeclaration decl, JavaSource source) {}
