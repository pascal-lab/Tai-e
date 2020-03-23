package pascal.dataflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.dataflow.analysis.constprop.CPTestSuite;
import pascal.dataflow.lattice.LatticeTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        LatticeTestSuite.class
})
public class DataFlowTestSuite {
}
