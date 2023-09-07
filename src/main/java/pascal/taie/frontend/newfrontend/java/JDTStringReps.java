package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class JDTStringReps {
    public static String getBinaryName(ITypeBinding binding) {
        return binding.getErasure().getBinaryName();
    }
}
