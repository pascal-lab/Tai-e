package pascal.taie.project;

import pascal.taie.config.Options;

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
}
