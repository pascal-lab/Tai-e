package sa.dataflow.constprop;

import soot.Local;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Data-flow information for constant propagation, which maps each variable to
 * a corresponding CPValue.
 */
public class FlowMap extends LinkedHashMap<Local, Value>
        implements sa.dataflow.lattice.FlowMap<Local, Value> {

    @Override
    public Value get(Object key) {
        return key instanceof Local
                ? getOrDefault(key, Value.getUndef())
                : null;
    }

    @Override
    public boolean update(Local key, Value value) {
        return Objects.equals(put(key, value), value);
    }
}
