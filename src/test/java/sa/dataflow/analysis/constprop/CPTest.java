package sa.dataflow.analysis.constprop;

import org.junit.Test;

import java.nio.file.Paths;

public class CPTest {

    @Test
    public void testRead() {
        ResultChecker checker = new ResultChecker();
        ResultChecker.ExpectedResult result = checker.readExpectedResult(
                Paths.get("analyzed/constprop/Simple-expected.txt")
        );
        System.out.println(result.getMap());
    }
}
