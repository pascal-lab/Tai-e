package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;

public interface FileContainer {

    List<AnalysisFile> files();

    List<FileContainer> containers();

    FileTime timeStamp();

    /**
     * Return FileName (with extension name)
     * e.g. a.jar --> a.jar
     */
    String fileName();

    /**
     * Return name (without extension name)
     * e.g. a.jar --> a
     */
    String className();

}
