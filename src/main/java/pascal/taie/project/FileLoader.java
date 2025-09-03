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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipException;

/**
 * Loads all class files in the given paths, and wraps them into
 * {@link ClassFile} and {@link FileContainer}.
 */
class FileLoader {

    private static final Logger logger = LogManager.getLogger(FileLoader.class);

    /**
     * Temp solution to handle non-root jar, e.g., b.jar nested in a.zip.
     */
    private List<FileContainer> nestedContainers;

    /**
     * Loads root containers from the given paths.
     * Typically, {@code paths} are the class paths of the program to analyze.
     */
    List<FileContainer> loadRootContainers(List<Path> paths) {
        nestedContainers = new CopyOnWriteArrayList<>();
        List<LoadAction> actions = new ArrayList<>();
        for (Path path : paths) {
            LoadAction action = new LoadAction(
                    new RootFileSystem(FileSystems.getDefault(), path),
                    path, null);
            ForkJoinPool.commonPool().execute(action);
            actions.add(action);
        }
        boolean hasError = false;
        List<FileContainer> containers = new ArrayList<>();
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
        containers.addAll(nestedContainers);
        return containers;
    }

    /**
     * Load Action for multithreaded loading.
     */
    private class LoadAction extends RecursiveAction {
        private final RootFileSystem root;
        private final Path path;
        private final FileContainer rootContainer;
        private final List<ClassFile> files = new ArrayList<>();
        private final List<FileContainer> containers = new ArrayList<>();

        private LoadAction(RootFileSystem root, Path path, FileContainer rootContainer) {
            this.root = root;
            this.path = path;
            this.rootContainer = rootContainer;
        }

        @Override
        protected void compute() {
            try {
                loadFile(root, path, rootContainer, files, containers);
            } catch (IOException e) {
                // avoid silent failure
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This class is used to store the root file system and the path of the file system.
     * <p>
     * There are basically two cases:
     *     <ul>
     *         <li>{@code fileSys == FileSystem.getDefault()}, and {@code path} is
     *         the path of the file on the disk</li>
     *         <li>{@code fileSys} is a zip file system, and {@code path} is
     *         the path of the zip file that creates the {@code fileSys}</li>
     *     </ul>
     * </p>
     *
     * @param fileSys the file system
     * @param path path of the file system
     */
    private record RootFileSystem(FileSystem fileSys, Path path) {

        /**
         * @return {@code true} if this is a zip file system.
         */
        private boolean isZip() {
            return fileSys != FileSystems.getDefault();
        }
    }

    /**
     * main method for loading files
     *
     * @param root            the root file system
     * @param path            the file to load
     * @param rootContainer   the root container of the file
     * @param files      when {@code path} is a file, execute {@code files.apply(path)}.
     * @param containers when {@code path} is a directory, execute {@code containers.apply(path)}.
     * @throws IOException if an I/O error occurs
     */
    private void loadFile(RootFileSystem root,
                          Path path,
                          FileContainer rootContainer,
                          List<ClassFile> files,
                          List<FileContainer> containers) throws IOException {
        if (Files.isDirectory(path)) {
            String name = path.getFileName().toString();
            if (name.equals("BOOT-INF") && root.isZip()) {
                // Special handing for SpringBoot fatjar
                // Load `classes` and `lib/*` as rootContainer
                Path classesPath = path.resolve("classes");
                if (Files.isDirectory(classesPath)) {
                    loadFile(root, classesPath, null, null, nestedContainers);
                }
                Path libsPath = path.resolve("lib");
                if (Files.isDirectory(libsPath)) {
                    try (Stream<Path> pathStream = Files.list(libsPath)) {
                        for (Path path1 : pathStream.toList()) {
                            loadFile(root, path1, null, null, null);
                        }
                    }
                }
            } else {
                List<ClassFile> subFiles = new ArrayList<>();
                List<FileContainer> subContainers = new ArrayList<>();
                FileContainer currentContainer = new DirContainer(name, subFiles, subContainers);
                if (rootContainer == null) {
                    // rootContainer == null means that the container currently
                    // being processed is a root.
                    rootContainer = currentContainer;
                }
                loadChildren(root, path, rootContainer, subFiles, subContainers);
                containers.add(currentContainer);
            }
        } else if (isZipFile(path)) {
            FileSystem fileSys;
            try {
                fileSys = FileSystemManager.getZipFileSys(path);
            } catch (ZipException e) {
                // Some error occur (maybe empty file, ...)
                // skip this zip file
                return;
            }
            String name = PathUtils.getClassSimpleName(path);
            List<ClassFile> subFiles = new ArrayList<>();
            List<FileContainer> subContainers = new ArrayList<>();
            FileContainer currentContainer;
            if (isJarFile(path)) {
                Manifest manifest = getManifest(fileSys);
                currentContainer = new JarContainer(name, subFiles, subContainers, manifest);
            } else {
                currentContainer = new ZipContainer(name, subFiles, subContainers);
            }
            if (rootContainer == null) {
                // rootContainer == null means that the container currently
                // being processed is a root.
                RootFileSystem newRoot = new RootFileSystem(fileSys, path);
                loadChildren(newRoot, fileSys.getPath("/"), currentContainer,
                        subFiles, subContainers);
                containers.add(currentContainer);
            }
        } else {
            Resource resource = makeResource(root, path);
            Path relativePath = getRelativePath(root, path);
            String className = PathUtils.getClassName(relativePath);
            if (isClassFile(path)) {
                files.add(new DotClassFile(className, resource, rootContainer));
            } else if (isJavaSourceFile(path)) {
                files.add(new DotJavaFile(className, resource, rootContainer));
            }
        }
    }

    private void loadChildren(RootFileSystem root,
                              Path path,
                              FileContainer rootContainer,
                              List<ClassFile> files,
                              List<FileContainer> containers) throws IOException {
        try (Stream<Path> paths = Files.list(path)) {
            List<LoadAction> actions = paths
                    .map(p -> new LoadAction(root, p, rootContainer))
                    .toList();
            // wait for all
            ForkJoinTask.invokeAll(actions);
            for (LoadAction action : actions) {
                files.addAll(action.files);
                containers.addAll(action.containers);
            }
        }
    }

    private static Resource makeResource(RootFileSystem root, Path path)
            throws IOException {
        if (root.isZip()) { // root is an entry of a zip file
            // path of [path] is on the disk, use lazy load
            if (root.path().getFileSystem() == FileSystems.getDefault()) {
                return new ZipEntryResource(path.toString(), root.fileSys(), null);
            } else {
                // path of [path] is an entry of a zip file, unzip the file of [path]
                byte[] cache = Files.readAllBytes(path);
                return new ZipEntryResource(path.toString(), null, cache);
            }
        } else { // otherwise it's a file on the disk
            return new FileResource(path);
        }
    }

    /**
     * Gets the manifest of a jar file.
     */
    @Nullable
    private static Manifest getManifest(FileSystem fs) throws IOException {
        Path p = fs.getPath("META-INF/MANIFEST.MF");
        if (Files.exists(p)) {
            return new Manifest(Files.newInputStream(p));
        } else {
            return null;
        }
    }

    private static Path getRelativePath(RootFileSystem root, Path path) {
        if (root.isZip()) {
            // just need to substrate root
            return root.fileSys().getPath("/").relativize(path);
        } else {
            return root.path().relativize(path);
        }
    }

    private static String getExt(Path path) {
        String fileName = path.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        return (i == -1) ? "" : fileName.substring(i + 1);
    }

    private static boolean isZipFile(Path path) {
        return getExt(path).equals("zip") || isJarFile(path);
    }

    private static boolean isJarFile(Path path) {
        return getExt(path).equals("jar");
    }

    private static boolean isClassFile(Path path) {
        return getExt(path).equals("class");
    }

    private static boolean isJavaSourceFile(Path path) {
        return getExt(path).equals("java");
    }
}
