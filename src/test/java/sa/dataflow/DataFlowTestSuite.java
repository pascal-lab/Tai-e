package sa.dataflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import sa.dataflow.analysis.AnalysisTestSuite;
import sa.dataflow.lattice.LatticeTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AnalysisTestSuite.class,
        LatticeTestSuite.class
})
public class DataFlowTestSuite {
}
