package sa.dataflow.lattice;

import java.util.Set;

interface FlowSetFactory<E> {

    FlowSet<E> getTop();

    FlowSet<E> getBottom();

    FlowSet<E> newFlowSet(Set<E> elements);
}
