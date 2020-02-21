package sa.pta.jimple;

import sa.pta.element.Obj;

class JimpleObj implements Obj {

    private Object allocation;

    private JimpleType type;

    private JimpleMethod allocationMethod;

    @Override
    public JimpleType getType() {
        return type;
    }

    @Override
    public Object getAllocationSite() {
        return allocation;
    }

    @Override
    public JimpleMethod getContainerMethod() {
        return allocationMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleObj jimpleObj = (JimpleObj) o;
        return allocation.equals(jimpleObj.allocation);
    }

    @Override
    public int hashCode() {
        return allocation.hashCode();
    }
}
