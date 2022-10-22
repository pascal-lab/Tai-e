package pascal.taie.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class FileLoader {

    private static final Logger logger = LogManager.getLogger(FileLoader.class);

    private static FileLoader obj;

    private Path getManifest(FileSystem fs) {
        return fs.getPath("META-INF/MANIFEST.MF");
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
                return new ZipEntryResource(p.p(), null, path.toString());
            } else {
                // path of [p] is an entry of a zip file, unzip the file of [path]
                byte[] cache = Files.readAllBytes(path);
                return new ZipEntryResource(p.p(), cache, path.toString());
            }
        }
    }

    private void loadChildren(Parent parent,
                              Path path,
                              List<AnalysisFile> files,
                              List<FileContainer> containers) throws IOException {
        try (var s = Files.list(path)) {
            for (var i : s.toList()) {
                loadFile(parent, i, files::add, containers::add);
            }
        }
    }

    public <T> void loadFile(
            Path path,
            Function<AnalysisFile, T> fileWorker,
            Function<FileContainer, T> containerWorker) throws IOException {
        loadFile(new Parent(FileSystems.getDefault(), path), path, fileWorker, containerWorker);
    }

    public <T> void loadFile(Parent parent,
                             Path path,
                             Function<AnalysisFile, T> fileWorker,
                             Function<FileContainer, T> containerWorker) throws IOException {
        if (!Files.exists(path)) {
            logger.warn(path + " is not exist");
        } else {
            if (Files.isDirectory(path)) {
                List<FileContainer> fileContainers = new ArrayList<>();
                List<AnalysisFile> files = new ArrayList<>();
                loadChildren(parent, path, files, fileContainers);
                FileTime time = Files.getLastModifiedTime(path);
                containerWorker.apply(new DirContainer(fileContainers, files, time));
            } else if (isZipFile(path)) {
                try (FileSystem fs = FileSystems.newFileSystem(path)) {
                    Parent newParent = new Parent(fs, path);
                    List<FileContainer> fileContainers = new ArrayList<>();
                    List<AnalysisFile> files = new ArrayList<>();
                    loadChildren(newParent, fs.getPath("/"), files, fileContainers);
                    FileTime time = Files.getLastModifiedTime(path);

                    if (isJarFile(path)) {
                        Manifest manifest = new Manifest(Files.newInputStream(getManifest(fs)));
                        containerWorker.apply(
                                new JarContainer(files, fileContainers, time, manifest));
                    } else {
                        containerWorker.apply(new ZipContainer(files, fileContainers, time));
                    }
                }
            } else {
                Resource r = mkResource(parent, path);
                FileTime time = Files.getLastModifiedTime(path);
                if (isClassFile(path)) {
                    fileWorker.apply(new ClassFile(getName(path), time, r));
                } else if (isJavaSourceFile(path)) {
                    fileWorker.apply(new JavaSourceFile(getName(path), time, r));
                } else {
                    fileWorker.apply(new OtherFile(path.getFileName().toString(), time, r));
                }
            }
        }
    }

    public static FileLoader get() {
        if (obj == null) {
            obj = new FileLoader();
        }
        return obj;
    }
}

