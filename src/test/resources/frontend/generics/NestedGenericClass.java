import java.util.List;
import java.util.Map;

/**
 * Class with nested generic types.
 */
public class NestedGenericClass<T> {
    private Map<String, List<T>> mapOfLists;
    private List<Map<String, T>> listOfMaps;
    private Map<T, Map<String, List<T>>> deeplyNested;

    public Map<String, List<T>> getMapOfLists() {
        return mapOfLists;
    }

    public <K> Map<K, List<T>> createMap(K key) {
        return null;
    }
}
