package pascal.dataflow.lattice;

import java.util.Set;

/**
 * Represents information for data-flow analysis.
 * A FlowSet is an element of a lattice.
 *
 * @param <E> Type for elements in this set.
 */
public interface FlowSet<E> extends Set<E> {

    /**
     * Unions other FlowSet into this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> union(FlowSet<E> other);

    /**
     * Intersects other FlowSet and this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> intersect(FlowSet<E> other);

    /**
     * Returns a duplication of this FlowSet.
     */
    FlowSet<E> duplicate();

    /**
     * Set this FlowSet to the same as the given one.
     */
    FlowSet<E> setTo(FlowSet<E> other);

}
