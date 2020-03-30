package bamboo.pta.jimple;

import bamboo.pta.element.Variable;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.InstanceLoad;
import bamboo.pta.statement.InstanceStore;
import soot.Local;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

class JimpleVariable implements Variable {

    private Local var;

    private JimpleType type;

    private JimpleMethod containerMethod;

    /**
     * Set of call sites where this variable is the base variable
     */
    private Set<Call> calls = Collections.emptySet();

    /**
     * Set of instance stores where this variable is the base variable
     */
    private Set<InstanceStore> stores = Collections.emptySet();

    /**
     * Set of instance loads where this variable is the base variable
     */
    private Set<InstanceLoad> loads = Collections.emptySet();

    public JimpleVariable(Local var, JimpleType type, JimpleMethod containerMethod) {
        this.var = var;
        this.type = type;
        this.containerMethod = containerMethod;
    }

    void addCall(Call call) {
        if (calls.isEmpty()) {
            calls = new LinkedHashSet<>(4);
        }
        calls.add(call);
    }

    void addStore(InstanceStore store) {
        if (stores.isEmpty()) {
            stores = new LinkedHashSet<>(4);
        }
        stores.add(store);
    }

    void addLoad(InstanceLoad load) {
        if (loads.isEmpty()) {
            loads = new LinkedHashSet<>(4);
        }
        loads.add(load);
    }

    @Override
    public JimpleType getType() {
        return type;
    }

    @Override
    public JimpleMethod getContainerMethod() {
        return containerMethod;
    }

    @Override
    public String getName() {
        return var.getName();
    }

    @Override
    public Set<Call> getCalls() {
        return calls;
    }

    @Override
    public Set<InstanceLoad> getLoads() {
        return loads;
    }

    @Override
    public Set<InstanceStore> getStores() {
        return stores;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleVariable that = (JimpleVariable) o;
        return var.equals(that.var);
    }

    @Override
    public int hashCode() {
        return var.hashCode();
    }

    @Override
    public String toString() {
        return containerMethod + "/" + var.getName();
    }
}
