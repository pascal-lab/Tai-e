package pascal.dataflow.analysis.constprop;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class CPTest {

    @Test
    public void testSimple() {
        test("Simple");
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

    @Test
    public void testVariousBinaryOp() {
        test("VariousBinaryOp");
    }

    private void test(String className) {
        Set<String> mismatches = ResultChecker.check(
                new String[]{ "-cp", "analyzed/constprop;analyzed/basic-classes.jar", className },
                "analyzed/constprop/" + className + "-expected.txt"
        );
        Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
    }
}
