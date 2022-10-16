package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

public class ZipContainer implements FileContainer {

    private final List<AnalysisFile> files;

    private final List<FileContainer> containers;

    private final FileTime time;


    public ZipContainer(List<AnalysisFile> files,
                        List<FileContainer> containers,
                        FileTime time) {
        this.files = files;
        this.containers = containers;
        this.time = time;
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
    public Optional<AnalysisFile> searchFile(String fileName) {
        return Optional.empty();
    }

    @Override
    public FileTime getTimeStamp() {
        return time;
    }
}
