package sa.dataflow.cp;

import sa.dataflow.lattice.AbstractFlowMap;

import java.util.LinkedHashMap;

/**
 * Data-flow information for constant propagation, which maps each variable to
 * a corresponding CPValue.
 */
public class FlowMap<K> extends AbstractFlowMap<K, Value> {

    public FlowMap() {
        meeter = new ValueMeeter();
        map = new LinkedHashMap<>();
    }

    @Override
    public Value get(K key) {
        return map.getOrDefault(key, Value.getUndef());
    }

    @Override
    public String toString() {
        return "CPFlowMap:" + map;
    }
}
