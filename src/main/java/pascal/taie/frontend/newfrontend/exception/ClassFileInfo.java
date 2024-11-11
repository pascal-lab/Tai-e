package pascal.taie.frontend.newfrontend.exception;

import pascal.taie.project.ClassFile;

import java.nio.file.Path;

public record ClassFileInfo(ClassFile file) {
    public String toString() {
        Path p = file.resource().getPath();
        return String.format("%s (%s in %s)",
                file.internalName(), p, file.rootContainer().fileName());
    }
}
