/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipException;

class FileLoader {

    private static final Logger logger = LogManager.getLogger(FileLoader.class);

    private static FileLoader obj;

    /**
     * temp solution to handle non-root jar (e.g. b.jar in a.zip),
     */
    private List<FileContainer> auxContainers = new ArrayList<>();

    // -------------------- Helper Functions (starts)--------------------

    /**
     * Get the manifest of a jar file
     *
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

    private Resource mkResource(RootFileSystem root, Path path) throws IOException {
        // fs is default means it's a file on the disk
        if (root.fs() == FileSystems.getDefault()) {
            return new FileResource(path);
        } else { // otherwise it's an entry of a zip file
            // path of [p] is on the disk, use lazy load
            if (root.p().getFileSystem() == FileSystems.getDefault()) {
                return new ZipEntryResource(root.p(), null, path.toString(), root.fs());
            } else {
                // path of [p] is an entry of a zip file, unzip the file of [path]
                byte[] cache = Files.readAllBytes(path);
                return new ZipEntryResource(root.p(), cache, path.toString(), null);
            }
        }
    }

    private Path getRelativePath(RootFileSystem root, Path p) {
        if (root.fs() == FileSystems.getDefault()) {
            return root.p().relativize(p);
        } else {
            // just need to substrate root
            return root.fs().getPath("/").relativize(p);
        }
    }
    // -------------------- Helper Functions (ends) --------------------

    // -------------------- Helper Classes (starts) --------------------

    /**
     * This class is used to store the root file system and the path of the file system.
     * <p>
     * There are basically two types:
     *     <ul>
     *         <li>{@code fs == FileSystem.getDefault()}, and {@code p} is the path of the file on the disk</li>
     *         <li>{@code fs} is a zip file system, and {@code p} is the path of the zip file that creates the {@code fs}</li>
     *     </ul>
     * </p>
     *
     * @param fs the file system
     * @param p path of the file system
     */
    record RootFileSystem(FileSystem fs, Path p) {
    }

    /**
     * Load Action for multithreaded loading
     */
    private class LoadAction extends RecursiveAction {
        private final RootFileSystem root;
        private final List<Path> paths;
        private final FileContainer rootContainer;
        private final List<ProgramFile> files;
        private final List<FileContainer> containers;

        LoadAction(RootFileSystem root, List<Path> paths, FileContainer rootContainer) {
            this.root = root;
            this.paths = paths;
            this.rootContainer = rootContainer;
            this.files = new ArrayList<>();
            this.containers = new ArrayList<>();
        }

        @Override
        protected void compute() {
            try {
                for (Path path : paths) {
                    loadFile(root, path, rootContainer, files::add, containers::add);
                }
            } catch (IOException e) {
                // avoid silent failure
                throw new RuntimeException(e);
            }
        }
    }
    // -------------------- Helper Classes (ends) --------------------

    // -------------------- Main Functions (starts) --------------------
    private void loadChildren(RootFileSystem root,
                              Path path,
                              FileContainer rootContainer,
                              List<ProgramFile> files,
                              List<FileContainer> containers) throws IOException {
        try (var s = Files.list(path)) {
            List<LoadAction> actions = new ArrayList<>();
            List<Path> current = new ArrayList<>();
            s.forEach((p) -> {
                LoadAction action = new LoadAction(root, List.of(p), rootContainer);
                actions.add(action);
            });
            // wait for all
            ForkJoinTask.invokeAll(actions);
            for (LoadAction action : actions) {
                files.addAll(action.files);
                containers.addAll(action.containers);
            }
        }
    }

    /**
     * main method for loading files
     *
     * @param root            the root file system
     * @param path            the file to load
     * @param rootContainer   the root container of the file
     * @param fileWorker      when {@code path} is a file, execute {@code fileWorker.apply(path)}.
     * @param containerWorker when {@code path} is a directory, execute {@code containerWorker.apply(path)}.
     * @throws IOException if an I/O error occurs
     */
    private <T> void loadFile(RootFileSystem root,
                              Path path,
                              FileContainer rootContainer,
                              Function<ProgramFile, T> fileWorker,
                              Function<FileContainer, T> containerWorker) throws IOException {
        if (Files.isDirectory(path)) {
            List<FileContainer> fileContainers = new ArrayList<>();
            List<ProgramFile> files = new ArrayList<>();
            FileTime time = Files.getLastModifiedTime(path);
            String name = path.getFileName().toString();
            if (name.equals("BOOT-INF") && root.fs() != FileSystems.getDefault()) {
                // spring boot fatjar
                // load `classes` and `lib/*` as rootContainer
                Path classesPath = path.resolve("classes");
                if (Files.isDirectory(classesPath)) {
                    loadFile(root, classesPath, null, null, auxContainers::add);
                }
                Path libsPath = path.resolve("lib");
                if (Files.isDirectory(libsPath)) {
                    try (Stream<Path> pathStream = Files.list(libsPath)) {
                        for (Path path1 : pathStream.toList()) {
                            loadFile(root, path1, null, (f) -> null, (d) -> null);
                        }
                    }
                }
            } else {
                FileContainer currentContainer = new DirContainer(fileContainers, files, time, name);
                if (rootContainer == null) {
                    // rootContainer == null means that the container currently
                    // being processed is a root.
                    rootContainer = currentContainer;
                }
                loadChildren(root, path, rootContainer, files, fileContainers);
                containerWorker.apply(currentContainer);
            }
        } else if (isZipFile(path)) {
            FileSystem fs;
            try {
                fs = FileSystemManager.get().newZipFS(path);
            } catch (ZipException e) {
                // Some error occur (maybe empty file, ...)
                // skip this zip file
                return;
            }
            List<FileContainer> fileContainers = new ArrayList<>();
            RootFileSystem newParent = new RootFileSystem(fs, path);
            List<ProgramFile> files = new ArrayList<>();
            FileTime time = Files.getLastModifiedTime(path);
            String name = PathUtils.getClassName(path);

            FileContainer currentContainer;
            if (isJarFile(path)) {
                Manifest manifest = getManifest(fs);
                currentContainer = new JarContainer(files, fileContainers, time, manifest, name);
            } else {
                currentContainer = new ZipContainer(files, fileContainers, time, name);
            }
            if (rootContainer == null) {
                // rootContainer == null means that the container currently
                // being processed is a root.
                rootContainer = currentContainer;
            } else {
                // skip
                return;
            }

            loadChildren(newParent, fs.getPath("/"), rootContainer, files, fileContainers);
            containerWorker.apply(currentContainer);
        } else {
            Resource r = mkResource(root, path);
            FileTime time = Files.getLastModifiedTime(path);
            Path relativePath = getRelativePath(root, path);
            String internalName = PathUtils.getInternalName(relativePath);
            if (isClassFile(path)) {
                fileWorker.apply(new DotClassFile(PathUtils.getClassName(path), internalName, time, r, rootContainer));
            } else if (isJavaSourceFile(path)) {
                fileWorker.apply(new DotJavaFile(PathUtils.getClassName(path), internalName, time, r, rootContainer));
            } else {
                fileWorker.apply(new OtherFile(path.getFileName().toString(), time, r, rootContainer));
            }
        }
    }

    /**
     * Load root containers from the given paths. Normally, the {@code paths} are
     * the classpaths of the program to be analyzed.
     */
    List<FileContainer> loadRootContainers(List<Path> paths) {
        this.auxContainers = new ArrayList<>();
        List<FileContainer> containers = new ArrayList<>();
        List<LoadAction> actions = new ArrayList<>();
        for (Path p : paths) {
            LoadAction action = new LoadAction(new RootFileSystem(FileSystems.getDefault(), p), List.of(p), null);
            ForkJoinPool.commonPool().execute(action);
            actions.add(action);
        }
        boolean hasError = false;
        for (LoadAction action : actions) {
            action.join();
            containers.addAll(action.containers);
            if (!action.files.isEmpty()) {
                hasError = true;
            }
        }
        if (hasError) {
            logger.warn("""
                    We have notice that you passing a non-jar file to classpath,
                    they will be ignored in the analysis.
                    Please check your classpath.""");
        }
        containers.addAll(auxContainers);
        return containers;
    }
    // -------------------- Main Functions (ends) --------------------

    public static FileLoader get() {
        if (obj == null) {
            obj = new FileLoader();
        }
        return obj;
    }

}

