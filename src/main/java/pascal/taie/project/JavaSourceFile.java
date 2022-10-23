package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public record JavaSourceFile(
        String className,
        FileTime timeStamp,
        Resource resource) implements AnalysisFile {

    @Override
    public String fileName() {
        return className + ".java";
    }

    public String getClassName() {
        return className;
    }
}
