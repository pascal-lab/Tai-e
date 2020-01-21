package sa.dataflow.constprop;

import soot.Local;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data-flow information for constant propagation, which maps each variable to
 * a corresponding CPValue.
 */
public class FlowMap implements sa.dataflow.lattice.FlowMap<Local, Value> {

    private Map<Local, Value> map = new LinkedHashMap<>();

    @Override
    public Value get(Local key) {
        return map.getOrDefault(key, Value.getUndef());
    }

    @Override
    public boolean put(Local key, Value value) {
        return Objects.equals(map.put(key, value), value);
    }

    @Override
    public boolean containsKey(Local key) {
        return map.containsKey(key);
    }

    @Override
    public Set<Local> keySet() {
        return map.keySet();
    }

    @Override
    public String toString() {
        return "FlowMap:" + map;
    }
}
