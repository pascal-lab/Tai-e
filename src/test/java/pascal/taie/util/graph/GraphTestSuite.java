package pascal.taie.util.graph;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        GraphTest.class,
        DominatorTest.class,
})
public class GraphTestSuite {
}
