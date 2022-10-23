package pascal.taie.project;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.jar.Manifest;

public class JarContainer extends ZipContainer {

    private final Manifest manifest;

    public JarContainer(List<AnalysisFile> files,
                        List<FileContainer> containers,
                        FileTime time,
                        Manifest manifest,
                        String name) {
        super(files, containers, time, name);
        this.manifest = manifest;
    }

    public Manifest getManifest() {
        return manifest;
    }

    @Override
    public String getFileName() {
        return this.name + ".jar";
    }
}
