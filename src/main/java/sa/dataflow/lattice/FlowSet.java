package sa.dataflow.lattice;

import java.util.Set;

/**
 * Represents information for data-flow analysis.
 * A FlowSet is an element of a lattice.
 *
 * @param <E> Type for elements in this set.
 */
public interface FlowSet<E> {

    /**
     * Adds an element to this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> add(E element);

    /**
     * Removes an element from this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> remove(E element);

    /**
     * Unions other FlowSet into this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> union(FlowSet<E> other);

    /**
     * Intersects other FlowSet and this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> intersect(FlowSet<E> other);

    /**
     * Returns the elements in this FlowSet.
     */
    Set<E> getElements();

    /**
     * Returns the size of this FlowSet.
     * Cannot called on universal set.
     */
    int size();

    /**
     * Returns if this FlowSet represents an empty set.
     */
    boolean isEmpty();

    /**
     * Returns if this FlowSet represents a universal set.
     */
    boolean isUniversal();
}
