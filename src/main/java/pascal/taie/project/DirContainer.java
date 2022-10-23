package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;

public class DirContainer implements FileContainer {

    private final List<FileContainer> containers;

    private final List<AnalysisFile> files;

    private final FileTime time;

    private final String name;

    public DirContainer(List<FileContainer> childContainers,
                        List<AnalysisFile> childFiles,
                        FileTime time,
                        String name) {
        this.containers = childContainers;
        this.files = childFiles;
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
        return name;
    }

    @Override
    public String getClassName() {
        return name;
    }
}
