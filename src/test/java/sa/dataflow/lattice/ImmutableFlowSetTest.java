package sa.dataflow.lattice;

public class ImmutableFlowSetTest extends FlowSetTest {

    public ImmutableFlowSetTest() {
        factory = new ImmutableFlowSet.Factory<>();
    }
}
