package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;

public class ZipContainer implements FileContainer {

    private final List<AnalysisFile> files;

    private final List<FileContainer> containers;

    private final FileTime time;

    protected final String name;


    public ZipContainer(List<AnalysisFile> files,
                        List<FileContainer> containers,
                        FileTime time,
                        String name) {
        this.files = files;
        this.containers = containers;
        this.time = time;
        this.name = name;
    }

    @Override
    public List<AnalysisFile> getFiles() {
        return files;
    }

    @Override
    public List<FileContainer> getContainers() {
        return containers;
    }

    @Override
    public FileTime getTimeStamp() {
        return time;
    }

    @Override
    public String getFileName() {
        return name + ".zip";
    }

    @Override
    public String getClassName() {
        return name;
    }
}
