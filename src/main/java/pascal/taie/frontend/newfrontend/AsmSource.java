package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;

public record AsmSource(
        ClassReader r
) implements ClassSource {

}
