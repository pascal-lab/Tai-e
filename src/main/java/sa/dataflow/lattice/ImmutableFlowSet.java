package sa.dataflow.lattice;

import sa.util.Canonicalizer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable implementation of FlowSet.
 * The instances of ImmutableFlowSet are canonicalized, i.e.,
 * the FlowSet with the same content has only one instance.
 * Every operation that changes a FlowSet will result another FlowSet.
 * Advantages of this implementation:
 *  1. fast equality testing
 *  2. saving memory usage
 *
 * @param <E> Type for elements in this set.
 */
class ImmutableFlowSet<E> extends FlowSet<E> {

    private Factory<E> factory;

    private int hashCode;

    private ImmutableFlowSet(Kind kind, Set<E> elements) {
        this.kind = kind;
        this.elements = elements;
    }

    @Override
    public FlowSet<E> add(E element) {
        if (isTop()) {
            return this;
        } else if (isBottom()) {
            return factory.newFlowSet(Collections.singleton(element));
        } else if (this.elements.contains(element)) {
            return this;
        } else {
            Set<E> elements = newSet(this.elements);
            elements.add(element);
            return factory.newFlowSet(elements);
        }
    }

    @Override
    public FlowSet<E> remove(E element) {
        if (isTop()) {
            throw new UnsupportedOperationException(
                    "Removing an element from TOP is not supported");
        } else if (isBottom()) {
            return this;
        } else if (this.elements.contains(element)){
            Set<E> elements = newSet(this.elements);
            elements.remove(element);
            return factory.newFlowSet(elements);
        } else {
            return this;
        }
    }

    @Override
    public FlowSet<E> union(FlowSet<E> other) {
        if (isTop()) {
            return this;
        } else if (isBottom()) {
            return other;
        } else {
            Set<E> elements = newSet(this.elements);
            elements.addAll(other.elements);
            return factory.newFlowSet(elements);
        }
    }

    @Override
    public FlowSet<E> intersect(FlowSet<E> other) {
        if (isTop()) {
            return other;
        } else if (isBottom()) {
            return this;
        } else {
            Set<E> elements = newSet(this.elements);
            elements.retainAll(other.elements);
            return factory.newFlowSet(elements);
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!factory.isCanonicalizing) {
            return this == obj;
        } else if (this == obj) {
            return true;
        } else if (!(obj instanceof ImmutableFlowSet)) {
            return false;
        }
        ImmutableFlowSet<?> other = (ImmutableFlowSet<?>) obj;
        return Objects.equals(kind, other.kind)
                && Objects.equals(elements, other.elements)
                && Objects.equals(factory, other.factory);
    }

    private Set<E> newSet(Set<E> elements) {
        return new HashSet<>(elements);
    }

    static class Factory<E> implements FlowSetFactory<E> {

        private Canonicalizer<FlowSet<E>> canonicalizer;

        private final FlowSet<E> TOP;

        private final FlowSet<E> BOTTOM;

        private boolean isCanonicalizing = false;

        public Factory() {
            canonicalizer = new Canonicalizer<>();
            TOP = canonicalize(new ImmutableFlowSet<>(Kind.TOP, null));
            BOTTOM = canonicalize(new ImmutableFlowSet<>(Kind.BOTTOM, null));
        }

        @Override
        public FlowSet<E> getTop() {
            return TOP;
        }

        @Override
        public FlowSet<E> getBottom() {
            return BOTTOM;
        }

        @Override
        public FlowSet<E> newFlowSet(Set<E> elements) {
            if (elements.isEmpty()) {
                return BOTTOM;
            } else {
                return canonicalize(new ImmutableFlowSet<>(Kind.NORMAL,
                        Collections.unmodifiableSet(elements)));
            }
        }

        private FlowSet<E> canonicalize(ImmutableFlowSet<E> fs) {
            isCanonicalizing = true;
            fs.factory = this;
            fs.hashCode = Objects.hash(fs.kind, fs.elements, fs.factory);
            FlowSet<E> cr = canonicalizer.canonicalize(fs);
            isCanonicalizing = false;
            return cr;
        }
    }
}
