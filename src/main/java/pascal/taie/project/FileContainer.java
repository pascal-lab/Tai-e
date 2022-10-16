package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

public interface FileContainer {

    List<AnalysisFile> getFiles();

    List<FileContainer> getContainers();

    Optional<AnalysisFile> searchFile(String fileName);

    FileTime getTimeStamp();

}
