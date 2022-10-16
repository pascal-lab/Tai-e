package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

public class DirContainer implements FileContainer{

    final private List<FileContainer> containers;

    final private List<AnalysisFile> files;

    final private FileTime time;

    public DirContainer(List<FileContainer> childContainers,
                        List<AnalysisFile> childFiles,
                        FileTime time) {
        this.containers = childContainers;
        this.files = childFiles;
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
