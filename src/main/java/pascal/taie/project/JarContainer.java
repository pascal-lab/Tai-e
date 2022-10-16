package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.jar.Manifest;

public class JarContainer extends ZipContainer {

    private final Manifest manifest;

    public JarContainer(List<AnalysisFile> files,
                        List<FileContainer> containers,
                        FileTime time,
                        Manifest manifest) {
        super(files, containers, time);
        this.manifest = manifest;
    }

    public Manifest getManifest() {
        return manifest;
    }
}
