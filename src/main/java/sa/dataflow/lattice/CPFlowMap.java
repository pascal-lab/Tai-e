package sa.dataflow.lattice;

import java.util.LinkedHashMap;

/**
 * Data-flow information for constant propagation, which maps each variable to
 * a corresponding CPValue.
 */
public class CPFlowMap<K> extends AbstractFlowMap<K, CPValue> {

    public CPFlowMap() {
        meeter = new CPValueMeeter();
        map = new LinkedHashMap<>();
    }

    @Override
    public CPValue get(K key) {
        return map.getOrDefault(key, CPValue.getUndef());
    }

    @Override
    public String toString() {
        return "CPFlowMap:" + map;
    }
}
