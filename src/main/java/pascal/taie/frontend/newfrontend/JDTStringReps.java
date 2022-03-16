package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.ITypeBinding;
import java.util.regex.Pattern;

public class JDTStringReps {
    public static String getBinaryName(ITypeBinding binding) {
        String name = binding.getErasure().getBinaryName();
        return name;
    }
}
