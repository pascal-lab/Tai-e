package pascal.taie.project;

import pascal.taie.config.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class OptionsProjectBuilder extends AbstractProjectBuilder {

    private final Options options;

    private Project project;

    public OptionsProjectBuilder(Options options) {
        this.options = options;
    }

    @Override
    protected String getMainClass() {
        return options.getMainClass();
    }

    @Override
    protected int getJavaVersion() {
        return options.getJavaVersion();
    }

    @Override
    protected List<String> getInputClasses() {
        return getInputClasses(options);
    }

    @Override
    protected List<FileContainer> getRootContainers() {
        return Stream.concat(
                project.getAppRootContainers().stream(),
                project.getLibRootContainers().stream()
        ).toList();
    }

    @Override
    public Project build() {
        try {
            List<String> appClassPaths;
            String appClassPath = options.getAppClassPath();
            if (appClassPath == null) {
                appClassPaths = List.of();
            } else {
                appClassPaths = Arrays.asList(appClassPath.split(File.pathSeparator));
            }

            List<String> libClassPaths;
            String classPath = getClassPath(options);
            if (classPath == null) {
                libClassPaths = List.of();
            } else {
                libClassPaths = new ArrayList<>(Arrays.asList(classPath.split(File.pathSeparator)));
                libClassPaths.removeAll(appClassPaths);
            }

            project = new Project(
                    getMainClass(),
                    getJavaVersion(),
                    getInputClasses(),
                    FileLoader.get().loadRootContainers(
                            appClassPaths.stream().distinct().map(Paths::get).toList()),
                    FileLoader.get().loadRootContainers(
                            Stream.concat(
                                libClassPaths.stream().distinct().map(Paths::get),
                                listJrtModule()).toList()));
            return project;
        } catch (IOException e) {
            // TODO: more info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Stream<Path> listJrtModule() throws IOException {
        if (options.getJreDir() == null) {
            return Stream.empty();
        }
        Path modulePath = FSManager.get().getJrtFs(Path.of(options.getJreDir()))
                .getPath("/modules");
        return Files.list(modulePath);
    }
}
