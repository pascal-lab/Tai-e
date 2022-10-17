package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public class JavaSourceFile implements AnalysisFile {

    private final String className;

    private final FileTime timeStamp;

    private final Resource resource;

    JavaSourceFile(String className, FileTime timeStamp, Resource resource) {
        this.className = className;
        this.timeStamp = timeStamp;
        this.resource = resource;
    }

    @Override
    public FileTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String getFileName() {
        return className + ".java";
    }

    public String getClassName() {
        return className;
    }
}
