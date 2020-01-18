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

    private HashFlowSet(Kind kind, Set<E> elements) {
        this.kind = kind;
        this.elements = elements;
    }

    @Override
    public FlowSet<E> add(E element) {
        if (!isUniversal()) {
            elements.add(element);
        }
        return this;
    }

    @Override
    public FlowSet<E> remove(E element) {
        if (isUniversal()) {
            throw new UnsupportedOperationException(
                    "Removing an element from a universal set is not supported");
        }
        elements.remove(element);
        return this;
    }

    @Override
    public FlowSet<E> union(FlowSet<E> other) {
        if (isUniversal() || other.isUniversal()) {
            kind = Kind.UNIVERSAL;
            elements = null;
        } else {
            elements.addAll(other.getElements());
        }
        return this;
    }

    @Override
    public FlowSet<E> intersect(FlowSet<E> other) {
        if (!other.isUniversal()) {
            if (!isUniversal()) {
                elements.retainAll(other.getElements());
            } else {
                kind = Kind.NORMAL;
                elements = new HashSet<>(other.getElements());
            }
        }
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, elements);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof HashFlowSet)) {
            return false;
        }
        HashFlowSet<?> other = (HashFlowSet<?>) obj;
        return Objects.equals(kind, other.kind)
                && Objects.equals(elements, other.elements);
    }

    static class Factory<E> extends FlowSetFactory<E> {

        @Override
        public FlowSet<E> getUniversalSet() {
            return new HashFlowSet<>(Kind.UNIVERSAL, null);
        }

        @Override
        public FlowSet<E> newFlowSet(Set<E> elements) {
            return new HashFlowSet<>(Kind.NORMAL, new HashSet<>(elements));
        }
    }
}
