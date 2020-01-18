package sa.dataflow.lattice;

import sa.util.Canonicalizer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents information for data-flow analysis. A FlowSet is an element of a lattice.
 * The instances of FlowSet are canonicalized. Every operation that changes
 * a FlowSet will result another FlowSet.
 *
 * @param <E> Type for elements in this set.
 */
class FlowSet<E> {

    private enum Kind {
        TOP, // the top element
        BOTTOM, // the bottom element
        NORMAL, // other lattice elements
    }

    private Kind kind;

    private Set<E> elements;

    private Factory<E> factory;

    private int hashCode;

    private FlowSet(Kind kind, Set<E> elements) {
        this.kind = kind;
        this.elements = elements;
    }

    /**
     * Returns if this FlowSet is the TOP value.
     */
    boolean isTop() {
        return this == factory.getTop();
    }

    /**
     * Returns if this FlowSet if the BOTTOM value.
     */
    boolean isBottom() {
        return this == factory.getBottom();
    }

    FlowSet<E> add(E element) {
        FlowSet<E> fs;
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

    FlowSet<E> remove(E element) {
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

    FlowSet<E> union(FlowSet<E> other) {
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

    FlowSet<E> intersect(FlowSet<E> other) {
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
        } else if (!(obj instanceof FlowSet)) {
            return false;
        }
        FlowSet<?> other = (FlowSet<?>) obj;
        return Objects.equals(kind, other.kind)
                && Objects.equals(elements, other.elements)
                && Objects.equals(factory, other.factory);
    }

    @Override
    public String toString() {
        if (isTop()) {
            return "FlowSet:TOP";
        } else if (isBottom()) {
            return "FlowSet:BOTTOM";
        } else {
            return "FlowSet:" + elements.toString();
        }
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
            TOP = canonicalize(new FlowSet<>(Kind.TOP, null));
            BOTTOM = canonicalize(new FlowSet<>(Kind.BOTTOM, null));
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
            return canonicalize(new FlowSet<>(Kind.NORMAL,
                    Collections.unmodifiableSet(elements)));
        }

        private FlowSet<E> canonicalize(FlowSet<E> fs) {
            isCanonicalizing = true;
            fs.factory = this;
            fs.hashCode = Objects.hash(fs.kind, fs.elements);
            FlowSet<E> cr = canonicalizer.canonicalize(fs);
            isCanonicalizing = false;
            return cr;
        }
    }
}
