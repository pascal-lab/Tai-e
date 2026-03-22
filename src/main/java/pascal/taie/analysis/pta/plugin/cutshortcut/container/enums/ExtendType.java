package pascal.taie.analysis.pta.plugin.cutshortcut.container.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ExtendType {
    MapToMap("MapToMap"), // Map.putAll(Map)
    ColToCol("ColToCol"), // Collection.addAll(Collection)
    MapKeySetToCol("MapKeySetToCol"), // Collection.addAll(Map.keySet())
    MapValuesToCol("MapValuesToCol"); // Collection.addAll(Map.values());

    private final String value;

    public String getValue() { return value; }

    ExtendType(String _value) { value = _value; }

    private final static Map<String, ExtendType> ValueToExtendType = Arrays.stream(values())
            .collect(Collectors.toMap(c -> c.value, Function.identity()));

    public static ExtendType getExtendType(String value) { return ValueToExtendType.getOrDefault(value, null); }
}
