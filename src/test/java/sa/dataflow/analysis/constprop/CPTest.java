package sa.dataflow.analysis.constprop;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class CPTest {

    @Test
    public void testSimple() {
        Set<String> mismatches = ResultChecker.check(
                new String[]{ "-cp", "analyzed/constprop;analyzed/basic-classes.jar", "Simple" },
                "analyzed/constprop/Simple-expected.txt"
        );
        Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
    }

    @Test
    public void testBinaryOp() {
        Set<String> mismatches = ResultChecker.check(
                new String[]{ "-cp", "analyzed/constprop;analyzed/basic-classes.jar", "BinaryOp" },
                "analyzed/constprop/BinaryOp-expected.txt"
        );
        Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
    }

    @Test
    public void testBranch() {
        Set<String> mismatches = ResultChecker.check(
                new String[]{ "-cp", "analyzed/constprop;analyzed/basic-classes.jar", "Branch" },
                "analyzed/constprop/Branch-expected.txt"
        );
        Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
    }
}
