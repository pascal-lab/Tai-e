package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public record OtherFile(String fileName,
                        FileTime timeStamp,
                        Resource resource,
                        FileContainer rootContainer
) implements AnalysisFile {
}
