package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.dataflow.inter.InterConstantPropagation;

public class InterCPAliasTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/alias";

    private static void test(String inputClass) {
        Tests.testDFA(inputClass, CLASS_PATH, InterConstantPropagation.ID,
                "alias-aware:true;pta:cspta", "-a", "cspta", "-a", "cg=algorithm:cspta");
    }

    @Test
    public void testSimpleField() {
        test("SimpleField");
    }

    @Test
    public void testMultiLoads() {
        test("MultiLoads");
    }

    @Test
    public void testMultiStores() {
        test("MultiStores");
    }

    @Test
    public void testMultiObjs() {
        test("MultiObjs");
    }

    @Test
    public void testInterprocedural() {
        test("Interprocedural");
    }

    @Test
    public void testInterprocedural2() {
        test("Interprocedural2");
    }
}
