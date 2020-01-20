package sa.dataflow.lattice;

import java.util.Map;
import java.util.Set;

public abstract class AbstractFlowMap<K, V> implements FlowMap<K, V> {

    protected Map<K, V> map;

    @Override
    public V get(K key) {
        return map.get(key);
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
