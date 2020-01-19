package sa.dataflow.lattice;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        HashFlowSetTest.class,
        ImmutableFlowSetTest.class,
        CPValueTest.class,
        CPValueMeeterTest.class
})
public class LatticeTest {
}
