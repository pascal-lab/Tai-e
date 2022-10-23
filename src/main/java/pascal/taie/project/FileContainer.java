package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;

public interface FileContainer {

    List<AnalysisFile> getFiles();

    List<FileContainer> getContainers();

    FileTime getTimeStamp();

    /**
     * Return FileName (with extension name)
     * e.g. a.jar --> a.jar
     */
    String getFileName();

    /**
     * Return name (without extension name)
     * e.g. a.jar --> a
     */
    String getClassName();

}
