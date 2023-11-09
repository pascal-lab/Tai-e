package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public record AsmSource(
        ClassReader r,
        boolean isApplication,
        int version,
        ClassNode node
) implements ClassSource {

    @Override
    public String getClassName() {
        String name;
        if (r == null) {
            name = node.name;
        } else {
            name = r.getClassName();
        }
        return Type.getObjectType(name).getClassName();
    }

    /**
     * @return the class file version of current class file
     */
    public int getClassFileVersion() {
        // some hack here
        // 6 is the offset of classfile version
        return version;
    }
}
