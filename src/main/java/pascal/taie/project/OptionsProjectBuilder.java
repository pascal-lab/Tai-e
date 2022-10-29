package pascal.taie.project;

import pascal.taie.config.Options;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class OptionsProjectBuilder extends AbstractProjectBuilder {

    private final Options options;

    public OptionsProjectBuilder(Options options) {
        this.options = options;
    }

    @Override
    protected String getMainClass() {
        return null;
    }

    @Override
    protected int getJavaVersion() {
        return 0;
    }

    @Override
    protected List<String> getInputClasses() {
        return null;
    }

    @Override
    protected List<FileContainer> getRootContainers() {
        return null;
    }

    @Override
    public Project build() {
        try {
            return new Project(options.getMainClass(),
                    options.getJavaVersion(),
                    options.getInputClasses(),
                    FileLoader.get().loadRootContainers(
                            Arrays.stream(options.getClassPath().split(";")).map(Paths::get).toList()),  // TODO: change to options.getAppClassPath() after modifying Options
                    FileLoader.get().loadRootContainers(
                            Arrays.stream(new String[]{}).map(Paths::get).toList()) // TODO: change to options.getLibClassPath() after modifying Options
            );
        } catch (IOException e) {
            // TODO: more info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
