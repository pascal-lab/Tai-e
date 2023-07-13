package pascal.taie.project;

import pascal.taie.config.Options;

import java.util.ArrayList;
import java.util.List;

public class MockOptions extends Options {
    private String classpath;

    private String mainClass;

    private List<String> inputClasses;

    private int javaVersion;

    public MockOptions() {
        inputClasses = new ArrayList<>();
        // should mainClass be added to inputClasses?
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setInputClasses(List<String> inputClasses) {
        this.inputClasses = inputClasses;
    }

    public void setJavaVersion(int version) {
        this.javaVersion = version;
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

    @Override
    public int getJavaVersion() {
        return javaVersion;
    }
}
