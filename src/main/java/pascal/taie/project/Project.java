package pascal.taie.project;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    public boolean isApp(AnalysisFile file) {
        return appRootContainers.contains(file.rootContainer());
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return the first file (with the same fully qualified name) found in the containerLists.
     * (QUESTION: how to define priority between different rootContainers?)
     */
    public AnalysisFile locate(String className) {
        ClassLocation classLocation = new ClassLocation(className);
        assert classLocation.hasNext();

        String root = classLocation.next();

        List<List<FileContainer>> rootContainersList = new ArrayList<>();
        rootContainersList.add(appRootContainers);
        rootContainersList.add(libRootContainers);

        for (List<FileContainer> rootContainers : rootContainersList) {
            for (FileContainer container : rootContainers) {
                // make sure to keep the order.
                if (container.className().equals(root)) {
                    AnalysisFile result = container.locate(classLocation);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return all the files with the full path.
     */
    public List<AnalysisFile> locateFiles(String className) {
        List<AnalysisFile> results = new ArrayList<>();

        ClassLocation classLocation = new ClassLocation(className);
        assert classLocation.hasNext();

        String root = classLocation.next();

        Consumer<FileContainer> get = c -> {
            AnalysisFile result = c.locate(classLocation);
            if (result != null) {
                results.add(result);
            }
        };

        appRootContainers.stream().filter(c -> c.className().equals(root)).forEach(get);

        libRootContainers.stream().filter(c -> c.className().equals(root)).forEach(get);

        return results;
    }
}
