package pascal.taie.analysis.pta.plugin.cutshortcut.container.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HostKind {
    MAP("Map"), MAP_ENTRY("MapEntry"),
    MAP_ENTRY_SET("MapEntrySet"), MAP_KEY_SET("MapKeySet"), MAP_VALUES("MapValues"),
    MAP_KEY_ITR("MapKeyIter"), MAP_ENTRY_ITR("MapEntryIter"), MAP_VALUE_ITR("MapValueIter"),

    COL("Col"),
    COL_ITR("ColIter");

    private String kind;

    HostKind(String _kind) { kind = _kind; }

    public String getKind() { return kind; }

    private final static Map<String, HostKind> NameToHostKind = Arrays.stream(values())
            .collect(Collectors.toMap(h -> h.kind, Function.identity()));

    public static HostKind getHostKind(String Name) { return NameToHostKind.get(Name); }
}
