package pascal.taie.project;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZipEntryResource implements Resource {

    @Nullable
    private byte[] cache;

    private final Path parent;

    private final String path;

    private final FileSystem fs;

    public ZipEntryResource(Path parent, byte[] cache, String path, FileSystem fs) {
        this.parent = parent;
        this.cache = cache;
        this.path = path;
        this.fs = fs;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (cache != null) {
            return new ByteArrayInputStream(cache);
        } else {
            // note: if reach here, parent must be on the disk
            try (FileSystem fs = FileSystems.newFileSystem(parent)) {
                return Files.newInputStream(fs.getPath(path));
            }
        }
    }

    @Override
    public byte[] getContent() throws IOException {
        if (cache == null) {
            // note: if reach here, parent must be on the disk
            cache = Files.readAllBytes(fs.getPath(path));
        }
        return cache;
    }

    @Override
    public Path getPath() {
        return fs.getPath(path);
    }
}
