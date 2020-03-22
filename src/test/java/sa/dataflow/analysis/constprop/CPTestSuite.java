package sa.dataflow.analysis.constprop;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTest.class,
        MeetValueTest.class,
        ValueTest.class,
})
public class CPTestSuite {
}
