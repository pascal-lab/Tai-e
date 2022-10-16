package pascal.taie.project;

import java.util.List;

public class ProjectRep {

    final private String mainClass;

    final private int javaVersion;

    final private List<String> inputClasses;

    final private FileContainer rootContainer;

    ProjectRep(String mainClass,
               int javaVersion,
               List<String> inputClasses,
               FileContainer rootContainer) {
        this.mainClass = mainClass;
        this.javaVersion = javaVersion;
        this.inputClasses = inputClasses;
        this.rootContainer = rootContainer;
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

    public FileContainer getRootContainer() {
        return rootContainer;
    }
}
