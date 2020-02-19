package sa.pta.statement;

import sa.pta.element.Variable;

/**
 * Represents a new statement: var = new T;
 */
public class Allocation implements Statement {

    private final Variable var;

    private final Object allocationSite;

    public Allocation(Variable var, Object allocationSite) {
        this.var = var;
        this.allocationSite = allocationSite;
    }

    public Variable getVar() {
        return var;
    }

    public Object getAllocationSite() {
        return allocationSite;
    }

    @Override
    public Kind getKind() {
        return Kind.ALLOCATION;
    }
}
