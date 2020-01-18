package sa.dataflow.lattice;

import java.util.Set;

/**
 * Represents information for data-flow analysis.
 * A FlowSet is an element of a lattice.
 *
 * @param <E> Type for elements in this set.
 */
public abstract class FlowSet<E> {

    protected Kind kind;

    protected Set<E> elements;

    /**
     * Adds an element to this FlowSet, returns the resulting FlowSet.
     */
    public abstract FlowSet<E> add(E element);

    /**
     * Removes an element from this FlowSet, returns the resulting FlowSet.
     */
    public abstract FlowSet<E> remove(E element);

    /**
     * Unions other FlowSet into this FlowSet, returns the resulting FlowSet.
     */
    public abstract FlowSet<E> union(FlowSet<E> other);

    /**
     * Intersects other FlowSet and this FlowSet, returns the resulting FlowSet.
     */
    public abstract FlowSet<E> intersect(FlowSet<E> other);

    /**
     * Returns the elements in this FlowSet.
     */
    public Set<E> getElements() {
        return elements;
    }

    /**
     * Returns if this FlowSet represents an empty set.
     */
    public boolean isEmpty() {
        return !isUniversal() && elements.isEmpty();
    }

    /**
     * Returns if this FlowSet represents a universal set.
     */
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
