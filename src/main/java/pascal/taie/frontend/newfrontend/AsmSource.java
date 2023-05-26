package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

public record AsmSource(
        ClassReader r,
        boolean isApplication
) implements ClassSource {

    @Override
    public String getClassName() {
        return Type.getObjectType(r.getClassName()).getClassName();
    }

    /**
     * @return the class file version of current class file
     */
    public int getClassFileVersion() {
        // some hack here
        // 6 is the offset of classfile version
        return r.readShort(6);
    }
}
