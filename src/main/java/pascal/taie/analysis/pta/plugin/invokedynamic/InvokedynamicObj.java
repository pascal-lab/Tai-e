package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.HashUtils;

import java.util.Optional;

public class InvokedynamicObj  implements Obj {

    private final Type type;

    private final InvokeDynamic allocation;

    private final JMethod container;

    InvokedynamicObj(Type type, InvokeDynamic allocation, JMethod container) {
        this.type = type;
        this.allocation = allocation;
        this.container = container;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public InvokeDynamic getAllocation() {
        return allocation;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.of(container);
    }

    @Override
    public Type getContainerType() {
        return container.getDeclaringClass().getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InvokedynamicObj invokedynamicObj = (InvokedynamicObj) o;
        return type.equals(invokedynamicObj.type) &&
                allocation.equals(invokedynamicObj.allocation);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(allocation, type);
    }

    @Override
    public String toString() {
        return "InvokedynamicObj{" +
                "type=" + type +
                ", allocation=" + allocation +
                ", container=" + container +
                '}';
    }
}
