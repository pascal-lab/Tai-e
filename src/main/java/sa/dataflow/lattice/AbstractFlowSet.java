package sa.dataflow.lattice;

import java.util.Set;

abstract class AbstractFlowSet<E> implements FlowSet<E> {

    protected Set<E> elements;

    @Override
    public Set<E> getElements() {
        return elements;
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public String toString() {
        return "FlowSet:" + elements.toString();
    }
}
