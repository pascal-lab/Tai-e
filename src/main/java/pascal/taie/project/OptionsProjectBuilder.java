package pascal.taie.project;

import pascal.taie.config.Options;

import java.io.IOException;
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
        return options.getInputClasses();
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
                appClassPaths = Arrays.asList(appClassPath.split(";"));
            }

            List<String> libClassPaths;
            String classPath = options.getClassPath();
            if (classPath == null) {
                libClassPaths = List.of();
            } else {
                libClassPaths = new ArrayList<>(Arrays.asList(classPath.split(";")));
                libClassPaths.removeAll(appClassPaths);
            }

            project = new Project(
                    options.getMainClass(),
                    options.getJavaVersion(),
                    options.getInputClasses(),
                    FileLoader.get().loadRootContainers(
                            appClassPaths.stream().map(Paths::get).toList()),
                    FileLoader.get().loadRootContainers(
                            libClassPaths.stream().map(Paths::get).toList())
            );
            return project;
        } catch (IOException e) {
            // TODO: more info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
