package sa.dataflow.lattice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

abstract class FlowSetFactory<E> {

    abstract FlowSet<E> getUniversalSet();

    FlowSet<E> newFlowSet(E... elements) {
        return newFlowSet(new HashSet<>(Arrays.asList(elements)));
    }

    abstract FlowSet<E> newFlowSet(Set<E> elements);
}
