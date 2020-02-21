package sa.pta.jimple;

import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Type;

public class JimpleObj implements Obj {

    private Object allocation;

    private Type type;

    private Method allocationMethod;

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getAllocationSite() {
        return allocation;
    }

    @Override
    public Method getAllocationMethod() {
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
