package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public class ClassFile implements AnalysisFile {

    private final String className;

    private final FileTime time;

    private final Resource resource;

    public ClassFile(String fileName, FileTime time, Resource resource) {
        this.className = fileName;
        this.time = time;
        this.resource = resource;
    }

    @Override
    public FileTime getTimeStamp() {
        return time;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String getFileName() {
        return className + ".class";
    }

    public String getClassName() {
        return className;
    }
}
