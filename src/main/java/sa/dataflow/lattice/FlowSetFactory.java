package sa.dataflow.lattice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class FlowSetFactory<E> {

    /**
     * Returns HashFlowSet by default.
     */
    public static <E> FlowSetFactory<E> getFactory() {
        return new HashFlowSet.Factory<>();
    }

    public FlowSet<E> newFlowSet(E... elements) {
        return newFlowSet(new HashSet<>(Arrays.asList(elements)));
    }

    public abstract FlowSet<E> newFlowSet(Set<E> elements);
}
