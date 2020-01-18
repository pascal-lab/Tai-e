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
class ImmutableFlowSet<E> extends AbstractFlowSet<E> {

    private Factory<E> factory;

    private int hashCode;

    private ImmutableFlowSet(Kind kind, Set<E> elements) {
        this.kind = kind;
        this.elements = elements;
    }

    @Override
    public FlowSet<E> add(E element) {
        if (isUniversal()) {
            return this;
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
        if (isUniversal()) {
            throw new UnsupportedOperationException(
                    "Removing an element from a universal set is not supported");
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
        if (isUniversal() || other.isUniversal()) {
            return factory.getUniversalSet();
        } else {
            Set<E> elements = newSet(this.elements);
            elements.addAll(other.getElements());
            return factory.newFlowSet(elements);
        }
    }

    @Override
    public FlowSet<E> intersect(FlowSet<E> other) {
        if (isUniversal()) {
            return other;
        } else if (other.isUniversal()) {
            return this;
        } else {
            Set<E> elements = newSet(this.elements);
            elements.retainAll(other.getElements());
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

    static class Factory<E> extends FlowSetFactory<E> {

        private Canonicalizer<FlowSet<E>> canonicalizer;

        private final FlowSet<E> UNIVERSAL;

        private boolean isCanonicalizing = false;

        public Factory() {
            canonicalizer = new Canonicalizer<>();
            UNIVERSAL = canonicalize(new ImmutableFlowSet<>(Kind.UNIVERSAL, null));
        }

        @Override
        public FlowSet<E> getUniversalSet() {
            return UNIVERSAL;
        }

        @Override
        public FlowSet<E> newFlowSet(Set<E> elements) {
            return canonicalize(new ImmutableFlowSet<>(Kind.NORMAL,
                    Collections.unmodifiableSet(elements)));
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
