package sa.dataflow.lattice;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        HashFlowSetTest.class,
        ImmutableFlowSetTest.class
})
public class LatticeTest {
}
