package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;

public class ZipContainer extends AbstractFileContainer {

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
        return name + ".zip";
    }

    @Override
    public String className() {
        return name;
    }
}
