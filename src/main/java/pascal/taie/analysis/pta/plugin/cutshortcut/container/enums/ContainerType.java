package pascal.taie.analysis.pta.plugin.cutshortcut.container.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ContainerType {
    MAP("Map"),
    COLLECTION("Col"),
    ENTRYSET("EntrySet"), // entrySet of map
    KEYSET("KeySet"), // keySet of map
    VALUES("Values"), // values of map
    ITER("Iter"),
    OTHER("OTHER");

    private final String typeName;

    ContainerType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    private final static Map<String, ContainerType> NameToContainerType = Arrays.stream(values())
            .collect(Collectors.toMap(c -> c.typeName, Function.identity()));

    public static ContainerType getTypeName(String Name) { return NameToContainerType.getOrDefault(Name, null); }
}
