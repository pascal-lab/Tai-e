package pascal.taie.project;

import java.util.Map;
import java.util.TreeMap;

public class SearchIndex {
    Map<String, AnalysisFile> index = new TreeMap<>();

    public void add(String binaryName, AnalysisFile file) {
        index.put(binaryName, file);
    }

    public AnalysisFile get(String fileName) {
        return index.get(fileName);
    }

    public static SearchIndex makeIndex(Project project) {
        SearchIndex index = new SearchIndex();
        for (FileContainer container : project.getAppRootContainers()) {
            index.trav("", container);
        }
        for (FileContainer container : project.getLibRootContainers()) {
            index.trav("", container);
        }
        return index;
    }

    private void trav(String currentName, FileContainer container) {
        for (AnalysisFile file : container.files()) {
            add(currentName + file.fileName(), file);
        }
        for (FileContainer subContainer : container.containers()) {
            trav(currentName + subContainer.fileName() + "/", subContainer);
        }
    }

    public AnalysisFile locate(String binaryName) {
        AnalysisFile klass = index.get(binaryName + ".class");
        if (klass != null) {
            return klass;
        } else {
            return index.get(binaryName + ".java");
        }
    }
}
