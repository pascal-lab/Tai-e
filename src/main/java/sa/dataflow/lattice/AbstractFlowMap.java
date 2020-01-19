package sa.dataflow.lattice;

import sa.dataflow.analysis.Meeter;

import java.util.Map;
import java.util.Set;

public abstract class AbstractFlowMap<K, V> implements FlowMap<K, V> {

    protected Meeter<V> meeter;

    protected Map<K, V> map;

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V meetAndPut(K key, V value) {
        return map.put(key, meeter.meet(get(key), value));
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }
}
