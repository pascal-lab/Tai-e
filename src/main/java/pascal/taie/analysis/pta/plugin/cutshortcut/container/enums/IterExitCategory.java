package pascal.taie.analysis.pta.plugin.cutshortcut.container.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum IterExitCategory {
    ColIter("ColIter"),
    MapGetValue("MapGetValue"),
    MapGetKey("MapGetKey");

    private final String category;

    IterExitCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    private final static Map<String, IterExitCategory> NameToCateGory = Arrays.stream(values())
            .collect(Collectors.toMap(c -> c.category, Function.identity()));

    public static IterExitCategory getCategory(String Name) { return NameToCateGory.getOrDefault(Name, null); }
}
