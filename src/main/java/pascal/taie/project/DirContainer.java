package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;

public class DirContainer extends AbstractFileContainer {

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
    public List<AnalysisFile> files() {
        return files;
    }

    @Override
    public List<FileContainer> containers() {
        return containers;
    }

    @Override
    public FileTime timeStamp() {
        return time;
    }

    @Override
    public String fileName() {
        return name;
    }

    @Override
    public String className() {
        return name;
    }
}
