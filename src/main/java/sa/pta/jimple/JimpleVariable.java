package sa.pta.jimple;

import sa.pta.element.CallSite;
import sa.pta.element.Variable;
import sa.pta.statement.InstanceLoad;
import sa.pta.statement.InstanceStore;
import soot.Local;

import java.util.Set;

class JimpleVariable implements Variable {

    private Local var;

    private JimpleType type;

    private JimpleMethod containerMethod;

    /**
     * Set of call sites where this variable is the base variable
     */
    private Set<CallSite> callSites;

    /**
     * Set of instance stores where this variable is the base variable
     */
    private Set<InstanceStore> stores;

    /**
     * Set of instance loads where this variable is the base variable
     */
    private Set<InstanceLoad> loads;

    public JimpleVariable(Local var, JimpleType type, JimpleMethod containerMethod) {
        this.var = var;
        this.type = type;
        this.containerMethod = containerMethod;
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
    public Set<CallSite> getCalls() {
        return callSites;
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
}
