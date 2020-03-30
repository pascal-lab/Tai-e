package bamboo.dataflow.analysis.constprop;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Set;

public class CPTest {

    @Test
    public void testSimple() {
        test("Simple");
    }

    @Test
    public void testSimpleBoolean() {
        test("SimpleBoolean");
    }

    @Test
    public void testBinaryOp() {
        test("BinaryOp");
    }

    @Test
    public void testBranchConstant() {
        test("BranchConstant");
    }

    @Test
    public void testBranchNAC() {
        test("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        test("BranchUndef");
    }

    @Test
    public void testInterprocedural() {
        test("Interprocedural");
    }

    @Test(expected = AssertionError.class)
    public void testBoolean() {
        test("Boolean");
    }

    @Test(expected = AssertionError.class)
    public void testVariousBinaryOp() {
        test("VariousBinaryOp");
    }

    private void test(String className) {
        String cp;
        if (new File("analyzed/constprop/").exists()) {
            cp = "analyzed/constprop/";
        } else {
            cp = "analyzed/";
        }
        Set<String> mismatches = ResultChecker.check(
                new String[]{ "-cp",
                        cp + File.pathSeparator + "analyzed/basic-classes.jar",
                        className },
                cp + className + "-expected.txt"
        );
        Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
    }
}
