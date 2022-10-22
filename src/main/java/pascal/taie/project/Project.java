package pascal.taie.project;

import java.util.List;

/**
 * Representation of a Java project.
 */
public class Project {

    final private String mainClass;

    final private int javaVersion;

    final private List<String> inputClasses;

    final private List<FileContainer> rootContainers;

    Project(String mainClass,
            int javaVersion,
            List<String> inputClasses,
            List<FileContainer> rootContainer) {
        this.mainClass = mainClass;
        this.javaVersion = javaVersion;
        this.inputClasses = inputClasses;
        this.rootContainers = rootContainer;
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

    public List<FileContainer> getRootContainers() {
        return rootContainers;
    }
}
