package sa.dataflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import sa.dataflow.constprop.CPTestSuite;
import sa.dataflow.lattice.LatticeTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        LatticeTestSuite.class
})
public class DataFlowTestSuite {
}
