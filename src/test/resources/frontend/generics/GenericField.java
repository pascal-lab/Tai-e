import java.util.List;
import java.util.Map;

/**
 * Class with generic fields.
 */
public class GenericField<T> {
    private T value;
    private List<T> list;
    private Map<String, T> map;
    private List<String> stringList;
    private Map<String, List<T>> nestedMap;
}
