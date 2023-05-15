package pascal.taie.project;

import pascal.taie.config.Options;

import java.io.IOException;
import java.nio.file.Paths;
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
            project = new Project(
                    options.getMainClass(),
                    options.getJavaVersion(),
                    options.getInputClasses(),
                    FileLoader.get().loadRootContainers(
                            Arrays.stream(options.getClassPath().split(";")).map(Paths::get).toList()),  // TODO: change to options.getAppClassPath() after modifying Options
                    FileLoader.get().loadRootContainers(
                            Arrays.stream(new String[]{}).map(Paths::get).toList()) // TODO: change to options.getLibClassPath() after modifying Options
            );
            return project;
        } catch (IOException e) {
            // TODO: more info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
