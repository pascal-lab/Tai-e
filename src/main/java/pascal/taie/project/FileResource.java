package pascal.taie.project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileResource implements Resource {

    final private Path path;
    private byte[] readCache;

    public FileResource(Path path) {
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (readCache != null) {
            return new ByteArrayInputStream(readCache);
        } else {
            return Files.newInputStream(path, StandardOpenOption.READ);
        }
    }

    @Override
    public byte[] getContent() throws IOException {
        if (readCache == null) {
            readCache = Files.readAllBytes(path);
        }
        return readCache;
    }
}
