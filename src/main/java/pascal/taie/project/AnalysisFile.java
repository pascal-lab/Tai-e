package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public interface AnalysisFile {

    FileTime timeStamp();

    Resource resource();

    /**
     * @return file name of this file (with extension name)
     */
    String fileName();

    FileContainer rootContainer();

}
