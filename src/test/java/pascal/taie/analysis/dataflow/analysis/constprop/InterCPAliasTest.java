package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.dataflow.inter.InterConstantPropagation;

public class InterCPAliasTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/alias";

    static void testInterCP(String inputClass) {
        Tests.testDFA(inputClass, CLASS_PATH, InterConstantPropagation.ID,
                "alias-aware:true;pta:cipta", "-a", "cipta", "-a", "cg=algorithm:cipta");
    }

    @Test
    public void testSimpleField() {
        testInterCP("SimpleField");
    }

    @Test
    public void testMultiLoads() {
        testInterCP("MultiLoads");
    }

    @Test
    public void testMultiStores() {
        testInterCP("MultiStores");
    }

    @Test
    public void testMultiObjs() {
        testInterCP("MultiObjs");
    }

    @Test
    public void testInterprocedural() {
        testInterCP("Interprocedural");
    }

    @Test
    public void testInterprocedural2() {
        testInterCP("Interprocedural2");
    }
}
