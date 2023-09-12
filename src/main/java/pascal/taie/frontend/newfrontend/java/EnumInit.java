package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.EnumConstantDeclaration;

import java.util.List;

record EnumInit(List<EnumConstantDeclaration> enumConsts) implements JavaClassInit {
}
