package pascal.taie.project;

import pascal.taie.config.Options;

import java.util.List;

public class MockOptions extends Options {
    private String classpath;

    private String mainClass;

    private List<String> inputClasses;

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setInputClasses(List<String> inputClasses) {
        this.inputClasses = inputClasses;
    }

    @Override
    public String getClassPath() {
        return classpath;
    }

    @Override
    public String getMainClass() {
        return mainClass;
    }

    @Override
    public List<String> getInputClasses() {
        return inputClasses;
    }
}
