package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;
import pascal.taie.analysis.TestUtils;
import pascal.taie.analysis.dataflow.ipa.IPConstantPropagation;

public class IPCPAliasTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/alias";

    static void testIPCP(String inputClass) {
        TestUtils.testDFA(inputClass, CLASS_PATH, IPConstantPropagation.ID,
                "alias-aware:true;pta:cipta", "-a", "cipta", "-a", "cg=pta:cipta");
    }

    @Test
    public void testSimpleField() {
        testIPCP("SimpleField");
    }

    @Test
    public void testMultiLoads() {
        testIPCP("MultiLoads");
    }

    @Test
    public void testMultiStores() {
        testIPCP("MultiStores");
    }

    @Test
    public void testMultiObjs() {
        testIPCP("MultiObjs");
    }

    @Test
    public void testInterprocedural() {
        testIPCP("Interprocedural");
    }

    @Test
    public void testInterprocedural2() {
        testIPCP("Interprocedural2");
    }
}
