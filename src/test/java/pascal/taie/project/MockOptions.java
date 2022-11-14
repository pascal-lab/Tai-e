package pascal.taie.project;

import pascal.taie.config.Options;

public class MockOptions extends Options {
    private String classpath;

    private String mainClass;

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public String getClassPath() {
        return classpath;
    }

    @Override
    public String getMainClass() {
        return mainClass;
    }
}
