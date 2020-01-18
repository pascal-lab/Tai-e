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
class HashFlowSet<E> extends FlowSet<E> {

    private HashFlowSet(Kind kind, Set<E> elements) {
        this.kind = kind;
        this.elements = elements;
    }

    @Override
    public FlowSet<E> add(E element) {
        if (isBottom()) {
            kind = Kind.NORMAL;
        }
        if (isNormal()) {
            elements.add(element);
        }
        return this;
    }

    @Override
    public FlowSet<E> remove(E element) {
        if (isTop()) {
            throw new UnsupportedOperationException(
                    "Removing an element from TOP is not supported");
        }
        if (isNormal()) {
            elements.remove(element);
            if (elements.isEmpty()) {
                kind = Kind.BOTTOM;
            }
        }
        return this;
    }

    @Override
    public FlowSet<E> union(FlowSet<E> other) {
        if (!isTop()) {


            elements.addAll(other.elements);
            if (!elements.isEmpty()) {
                kind = Kind.NORMAL;
            }
        }
        return this;
    }

    @Override
    public FlowSet<E> intersect(FlowSet<E> other) {
        if (isTop()) {
            elements = new HashSet<>(other.elements);
        } else {
            elements.retainAll(other.elements);
        }
        if (elements.isEmpty()) {
            kind = Kind.BOTTOM;
        }

        if (isTop()) {
            kind = Kind.NORMAL;

        }
        if (isNormal()) {
            elements.retainAll(other.elements);
        }
        if (elements.isEmpty()) {
            kind = Kind.BOTTOM;
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

    static class Factory<E> implements FlowSetFactory<E> {

        @Override
        public FlowSet<E> getTop() {
            return new HashFlowSet<>(Kind.TOP, null);
        }

        @Override
        public FlowSet<E> getBottom() {
            return new HashFlowSet<>(Kind.BOTTOM, new HashSet<>());
        }

        @Override
        public FlowSet<E> newFlowSet(Set<E> elements) {
            if (elements.isEmpty()) {
                return getBottom();
            }
            return new HashFlowSet<>(Kind.NORMAL, new HashSet<>(elements));
        }
    }
}
