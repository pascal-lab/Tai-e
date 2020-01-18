package sa.dataflow;

import sa.util.Canonicalizer;
import sa.util.DeepImmutable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents information for data-flow analysis. A FlowSet is an element of a lattice.
 * The instances of FlowSet are immutable and canonicalized.
 *
 * @param <E> Type for elements in this set.
 */
class FlowSet<E> implements DeepImmutable {

    private static Canonicalizer<FlowSet<?>> canonicalizer = new Canonicalizer<>();

    private enum Kind {
        TOP, // the top element
        BOTTOM, // the bottom element
        NORMAL, // other lattice elements
    }

    private static final FlowSet<?> TOP = canonicalize(new FlowSet<>(Kind.TOP, null));

    private static final FlowSet<?> BOTTOM = canonicalize(new FlowSet<>(Kind.BOTTOM, null));

    private static boolean isCanonicalizing = false;

    private Kind kind;

    private Set<E> elements;

    private int hashCode;

    private FlowSet(Kind kind, Set<E> elements) {
        this.kind = kind;
        this.elements = elements;
        this.hashCode = 0;
    }

    static FlowSet<?> getTop() {
        return TOP;
    }

    static FlowSet<?> getBottom() {
        return BOTTOM;
    }

    static <E> FlowSet<E> newFlowSet(Set<E> elements) {
        return canonicalize(new FlowSet<E>(Kind.NORMAL, elements));
    }

    /**
     * Returns if this FlowSet is the TOP value.
     */
    boolean isTop() {
        return this == TOP;
    }

    /**
     * Returns if this FlowSet if the BOTTOM value.
     */
    boolean isBottom() {
        return this == BOTTOM;
    }

    FlowSet<E> add(E element) {
        FlowSet<E> fs;
        if (isTop()) {
            fs = this;
        } else if (isBottom()) {
            fs = new FlowSet<>(Kind.NORMAL, Collections.singleton(element));
        } else if (this.elements.contains(element)) {
            fs = this;
        } else {
            Set<E> elements = newSet(this.elements);
            elements.add(element);
            fs = new FlowSet<>(Kind.NORMAL, elements);
        }
        return canonicalize(fs);
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
            return canonicalize(new FlowSet<>(Kind.NORMAL, elements));
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
            return canonicalize(new FlowSet<>(Kind.NORMAL, elements));
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
            return canonicalize(new FlowSet<>(Kind.NORMAL, elements));
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!isCanonicalizing) {
            return this == obj;
        } else if (this == obj) {
            return true;
        } else if (!(obj instanceof FlowSet)) {
            return false;
        }
        FlowSet<?> other = (FlowSet<?>) obj;
        return Objects.equals(kind, other.kind)
                && Objects.equals(elements, other.elements);
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

    private static <E> Set<E> newSet(Set<E> elements) {
        return new HashSet<>(elements);
    }

    private static <E> FlowSet<E> canonicalize(FlowSet<E> flowSet) {
        isCanonicalizing = true;
        FlowSet<E> r = new FlowSet<>(Kind.NORMAL,
                Collections.unmodifiableSet(flowSet.elements));
        r.hashCode = Objects.hash(r.kind, r.elements);
        @SuppressWarnings("unchecked")
        FlowSet<E> cr = (FlowSet<E>) canonicalizer.canonicalize(r);
        isCanonicalizing = false;
        return cr;
    }
}
