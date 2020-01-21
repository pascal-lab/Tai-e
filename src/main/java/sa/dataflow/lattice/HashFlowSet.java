package sa.dataflow.lattice;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * FlowSet implementation which uses hash set to store data flow.
 * This implementation is simple and fast for various set operations.
 *
 * @param <E> Type for elements in this set.
 */
class HashFlowSet<E> extends AbstractFlowSet<E> {

    private HashFlowSet(Set<E> elements) {
        this.elements = elements;
    }

    @Override
    public FlowSet<E> add(E element) {
        elements.add(element);
        return this;
    }

    @Override
    public FlowSet<E> remove(E element) {
        elements.remove(element);
        return this;
    }

    @Override
    public FlowSet<E> union(FlowSet<E> other) {
        elements.addAll(other.getElements());
        return this;
    }

    @Override
    public FlowSet<E> intersect(FlowSet<E> other) {
        elements.retainAll(other.getElements());
        return this;
    }

    @Override
    public FlowSet<E> duplicate() {
        return new HashFlowSet<>(new HashSet<>(elements));
    }

    @Override
    public FlowSet<E> setTo(FlowSet<E> other) {
        elements.clear();
        elements.addAll(other.getElements());
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof HashFlowSet)) {
            return false;
        }
        HashFlowSet<?> other = (HashFlowSet<?>) obj;
        return Objects.equals(elements, other.elements);
    }

    static class Factory<E> extends FlowSetFactory<E> {

        @Override
        public FlowSet<E> newFlowSet(Set<E> elements) {
            return new HashFlowSet<>(new HashSet<>(elements));
        }
    }
}
