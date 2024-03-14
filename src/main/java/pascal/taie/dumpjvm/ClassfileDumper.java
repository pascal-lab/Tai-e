package pascal.taie.dumpjvm;

import pascal.taie.language.classes.JClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassfileDumper {
    public static void dump(Path cp, JClass jClass) {
        byte[] classfileBuffer = new BytecodeEmitter().emit(jClass);
        Path classfilePath = cp.resolve(
                BytecodeEmitter.computeInternalName(jClass) + ".class");
        try {
            Files.createDirectories(classfilePath.getParent());
            Files.write(classfilePath, classfileBuffer);
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot write classfile to " + classfilePath, e);
        }
    }
}
