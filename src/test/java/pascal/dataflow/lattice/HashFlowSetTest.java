package pascal.dataflow.lattice;

import java.util.Arrays;

public class HashFlowSetTest extends FlowSetTest {

    @Override
    protected FlowSet<String> newFlowSet(String... strings) {
        return new HashFlowSet<>(Arrays.asList(strings));
    }
}
