package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

public record AsmSource(
        ClassReader r
) implements ClassSource {

    @Override
    public String getClassName() {
        return Type.getObjectType(r.getClassName()).getClassName();
    }
}
