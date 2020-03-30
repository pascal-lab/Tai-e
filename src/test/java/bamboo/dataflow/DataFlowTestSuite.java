package bamboo.dataflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import bamboo.dataflow.analysis.constprop.CPTestSuite;
import bamboo.dataflow.lattice.LatticeTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        LatticeTestSuite.class
})
public class DataFlowTestSuite {
}
