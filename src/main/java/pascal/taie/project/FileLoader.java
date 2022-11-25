package pascal.taie.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

public class FileLoader {

    private static final Logger logger = LogManager.getLogger(FileLoader.class);

    private static FileLoader obj;

    /**
     * temp solution to handle non-root jar (e.g. b.jar in a.zip),
     */
    private List<FileContainer> auxContainers = new ArrayList<>();

    /**
     * Get the manifest of a jar file
     * @return may be null (no Manifest include)
     */
    private @Nullable Manifest getManifest(FileSystem fs) throws IOException {
        Path p = fs.getPath("META-INF/MANIFEST.MF");
        if (Files.exists(p)) {
            return new Manifest(Files.newInputStream(p));
        } else {
            return null;
        }
    }

    /**
     * for this class only
     *
     * @return ext name of p
     */
    private String getExt(Path p) {
        String s = p.getFileName().toString();
        int dotIndex = s.lastIndexOf('.');
        return (dotIndex == -1) ? "" : s.substring(dotIndex + 1);
    }

    private String getName(Path p) {
        String s = p.getFileName().toString();
        int dotIndex = s.lastIndexOf('.');
        return (dotIndex == -1) ? s : s.substring(0, dotIndex);
    }

    private boolean isClassFile(Path p) {
        return getExt(p).equals("class");
    }

    private boolean isZipFile(Path p) {
        return getExt(p).equals("zip") || get().isJarFile(p);
    }

    private boolean isJarFile(Path p) {
        return getExt(p).equals("jar");
    }

    private boolean isJavaSourceFile(Path p) {
        return getExt(p).equals("java");
    }

    private Resource mkResource(Parent p, Path path) throws IOException {
        // fs is default means it's a file on the disk
        if (p.fs() == FileSystems.getDefault()) {
            return new FileResource(path);
        }
        // otherwise it's an entry of a zip file
        else {
            // path of [p] is on the disk, use lazy load
            if (p.p().getFileSystem() == FileSystems.getDefault()) {
                return new ZipEntryResource(p.p(), null, path.toString(), p.fs());
            } else {
                // path of [p] is an entry of a zip file, unzip the file of [path]
                byte[] cache = Files.readAllBytes(path);
                return new ZipEntryResource(p.p(), cache, path.toString(), null);
            }
        }
    }

    private void loadChildren(Parent parent,
                              Path path,
                              FileContainer rootContainer,
                              List<AnalysisFile> files,
                              List<FileContainer> containers) throws IOException {
        try (var s = Files.list(path)) {
            for (var i : s.toList()) {
                loadFile(parent, i, rootContainer, files::add, containers::add);
            }
        }
    }

    public <T> void loadFile(
            Path path,
            FileContainer rootContainer,
            Function<AnalysisFile, T> fileWorker,
            Function<FileContainer, T> containerWorker) throws IOException {
        loadFile(new Parent(FileSystems.getDefault(), path), path, rootContainer, fileWorker, containerWorker);
    }

    public <T> void loadFile(Parent parent,
                             Path path,
                             FileContainer rootContainer,
                             Function<AnalysisFile, T> fileWorker,
                             Function<FileContainer, T> containerWorker) throws IOException {
        if (!Files.exists(path)) {
            logger.warn(path + " is not exist");
        } else {
            if (Files.isDirectory(path)) {
                List<FileContainer> fileContainers = new ArrayList<>();
                List<AnalysisFile> files = new ArrayList<>();
                FileTime time = Files.getLastModifiedTime(path);
                String name = path.getFileName().toString();
                FileContainer currentContainer = new DirContainer(fileContainers, files, time, name);
                if (rootContainer == null) {
                    // rootContainer == null means that the container currently
                    // being processed is a root.
                    rootContainer = currentContainer;
                }

                loadChildren(parent, path, rootContainer, files, fileContainers);
                containerWorker.apply(currentContainer);
            }  else if (isZipFile(path)) {
                FileSystem fs;
                try {
                    fs = FSManager.get().newZipFS(path);
                } catch (ZipException e) {
                    // Some error occur (maybe empty file, ...)
                    // skip this zip file
                    return;
                }
                List<FileContainer> fileContainers = new ArrayList<>();
                Parent newParent = new Parent(fs, path);
                List<AnalysisFile> files = new ArrayList<>();
                FileTime time = Files.getLastModifiedTime(path);
                String name = getName(path);

                FileContainer currentContainer;
                if (isJarFile(path)) {
                    Manifest manifest = getManifest(fs);
                    currentContainer = new JarContainer(files, fileContainers, time, manifest, name);
                    auxContainers.add(currentContainer);
                } else {
                    currentContainer = new ZipContainer(files, fileContainers, time, name);
                }
                if (rootContainer == null) {
                    // rootContainer == null means that the container currently
                    // being processed is a root.
                    rootContainer = currentContainer;
                }

                loadChildren(newParent, fs.getPath("/"), rootContainer, files, fileContainers);
                containerWorker.apply(currentContainer);
            } else {
                Resource r = mkResource(parent, path);
                FileTime time = Files.getLastModifiedTime(path);
                if (isClassFile(path)) {
                    fileWorker.apply(new ClassFile(getName(path), time, r, rootContainer));
                } else if (isJavaSourceFile(path)) {
                    fileWorker.apply(new JavaSourceFile(getName(path), time, r, rootContainer));
                } else {
                    fileWorker.apply(new OtherFile(path.getFileName().toString(), time, r, rootContainer));
                }
            }
        }
    }

    public List<FileContainer> loadRootContainers(List<Path> paths) throws IOException {
        this.auxContainers = new ArrayList<>();
        List<FileContainer> containers = new ArrayList<>();
        for (var p : paths) {
            loadFile(p,
                    null,
                    i -> {throw new IllegalArgumentException("no file in classPaths");},
                    containers::add);
        }
        containers.addAll(auxContainers);
        return containers;
    }

    public static FileLoader get() {
        if (obj == null) {
            obj = new FileLoader();
        }
        return obj;
    }
}

