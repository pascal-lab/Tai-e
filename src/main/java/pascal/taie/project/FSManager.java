package pascal.taie.project;

import pascal.taie.util.collection.Maps;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

// TODO: close all fileSystem at right time
public class FSManager {
    Map<Path, FileSystem> fsMap;

    private FSManager() {
        fsMap = Maps.newMap();
    }

    public FileSystem newZipFS(Path path) {
        return fsMap.computeIfAbsent(path, p -> {
            try {
                return FileSystems.newFileSystem(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static FSManager manager;
    static public FSManager get() {
        if (manager == null) {
            manager = new FSManager();
        }
        return manager;
    }
}
