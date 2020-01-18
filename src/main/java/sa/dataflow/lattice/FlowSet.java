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
     * Returns if this FlowSet is the TOP value.
     */
    public boolean isTop() {
        return kind == Kind.TOP;
    }

    /**
     * Returns if this FlowSet is the BOTTOM value.
     */
    public boolean isBottom() {
        return kind == Kind.BOTTOM;
    }

    /**
     * Returns if this FlowSet is neither TOP nor BOTTOM.
     */
    public boolean isNormal() {
        return kind != Kind.TOP && kind != Kind.BOTTOM;
    }

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
}
