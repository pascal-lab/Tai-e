package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public record ClassFile(
        String className,
        FileTime timeStamp,
        Resource resource
) implements AnalysisFile {

    @Override
    public String fileName() {
        return className + ".class";
    }

    public String getClassName() {
        return className;
    }
}
