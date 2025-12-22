package pascal.taie.frontend.java;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import pascal.taie.frontend.java.closeworld.DependencyAnalysisTest;
import pascal.taie.frontend.java.generics.GenericsTest;
import pascal.taie.frontend.java.project.ProjectTest;

@Suite
@SelectClasses({
        ProjectTest.class,
        DependencyAnalysisTest.class,
        GenericsTest.class,
})
public class FrontendTestSuite {
}
