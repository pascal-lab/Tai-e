package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public class OtherFile implements AnalysisFile {

    private final String fileName;

    private final Resource resource;

    private final FileTime timeStamp;

    public OtherFile(String fileName, FileTime timeStamp, Resource resource) {
        this.fileName = fileName;
        this.resource = resource;
        this.timeStamp = timeStamp;
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
        return fileName;
    }
}
