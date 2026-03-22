package pascal.taie.analysis.pta.plugin.cutshortcut.container.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ContExitCategory {
    MapKey("MapKey"),
    MapValue("MapValue"),
    ColValue("ColValue");

    private final String category;

    ContExitCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    private final static Map<String, ContExitCategory> NameToCateGory = Arrays.stream(values())
            .collect(Collectors.toMap(c -> c.category, Function.identity()));

    public static ContExitCategory getCategory(String Name) { return NameToCateGory.getOrDefault(Name, null); }
}
