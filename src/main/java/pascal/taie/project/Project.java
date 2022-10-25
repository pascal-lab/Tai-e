package pascal.taie.project;

import java.util.List;

/**
 * Representation of a Java project.
 */
public class Project {

    final private String mainClass;

    final private int javaVersion;

    final private List<String> inputClasses;

    final private List<FileContainer> appRootContainers;

    final private List<FileContainer> libRootContainers;

    Project(String mainClass,
            int javaVersion,
            List<String> inputClasses,
            List<FileContainer> appRootContainers,
            List<FileContainer> libRootContainers) {
        this.mainClass = mainClass;
        this.javaVersion = javaVersion;
        this.inputClasses = inputClasses;
        this.appRootContainers = appRootContainers;
        this.libRootContainers = libRootContainers;
    }

    public String getMainClass() {
        return mainClass;
    }

    public int getJavaVersion() {
        return javaVersion;
    }

    public List<String> getInputClasses() {
        return inputClasses;
    }

    public List<FileContainer> getAppRootContainers() {
        return appRootContainers;
    }

    public List<FileContainer> getLibRootContainers() {
        return libRootContainers;
    }

    public AnalysisFile locate(String className) {
        return null;
    }
}
