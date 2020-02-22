package sa.pta.analysis.heap;

import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Type;

/**
 * A general implementation of Obj.
 */
class ObjImpl implements Obj {

    private final Object allocation;

    private final Type type;

    private final Method containerMethod;

    ObjImpl(Object allocation, Type type, Method containerMethod) {
        this.allocation = allocation;
        this.type = type;
        this.containerMethod = containerMethod;
    }

    @Override
    public Object getAllocationSite() {
        return allocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Method getContainerMethod() {
        return containerMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjImpl obj = (ObjImpl) o;
        return allocation.equals(obj.allocation);
    }

    @Override
    public int hashCode() {
        return allocation.hashCode();
    }

    @Override
    public String toString() {
        return allocation.toString();
    }
}
