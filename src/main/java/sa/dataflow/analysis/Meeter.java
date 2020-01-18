package sa.dataflow.analysis;

/**
 * Meet operator.
 * @param <V>
 */
public interface Meeter<V> {

    V meet(V v1, V v2);
}
