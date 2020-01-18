package sa.dataflow.lattice;

import java.util.Set;

abstract class AbstractFlowSet<E> implements FlowSet<E> {

    protected Kind kind;

    protected Set<E> elements;

    @Override
    public Set<E> getElements() {
        return elements;
    }

    @Override
    public int size() {
        if (isUniversal()) {
            throw new UnsupportedOperationException(
                    "FlowSet.size() on universal set is not supported");
        }
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return !isUniversal() && elements.isEmpty();
    }

    @Override
    public boolean isUniversal() {
        return kind == Kind.UNIVERSAL;
    }

    @Override
    public String toString() {
        if (isUniversal()) {
            return "FlowSet:Universal";
        } else {
            return "FlowSet:" + elements.toString();
        }
    }
}
