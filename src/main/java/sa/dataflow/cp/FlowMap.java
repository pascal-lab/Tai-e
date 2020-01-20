package sa.dataflow.cp;

import sa.dataflow.lattice.AbstractFlowMap;
import soot.Local;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Data-flow information for constant propagation, which maps each variable to
 * a corresponding CPValue.
 */
public class FlowMap extends AbstractFlowMap<Local, Value> {

    public FlowMap() {
        map = new LinkedHashMap<>();
    }

    @Override
    public Value get(Local key) {
        return map.getOrDefault(key, Value.getUndef());
    }

    @Override
    public boolean put(Local key, Value value) {
        return Objects.equals(map.put(key, value), value);
    }

    @Override
    public String toString() {
        return "FlowMap:" + map;
    }
}
