package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public class ClassFile implements AnalysisFile {

    String fileName;

    FileTime time;

    Resource resource;

    public ClassFile(String fileName, FileTime time, Resource resource) {
        this.fileName = fileName;
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
        return fileName;
    }
}
