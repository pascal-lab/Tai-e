package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.dataflow.inter.InterConstantPropagation;

public class InterCPAliasTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/alias";

    private static void test(String inputClass) {
        Tests.testDFA(inputClass, CLASS_PATH, InterConstantPropagation.ID,
                "edge-refine:false;alias-aware:true;pta:cspta",
                "-a", "cspta=cs:2-obj", "-a", "cg=algorithm:cspta"
                //, "-a", "icfg=dump:true" // <-- uncomment this code if you want
                                           // to output ICFGs for the test cases
        );
    }

    // Tests instance field
    @Test
    public void testInstanceField() {
        test("InstanceField");
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

    @Test
    public void testInheritedField() {
        test("InheritedField");
    }

    @Test
    public void testFieldCorner() {
        test("FieldCorner");
    }

    // Tests static field
    @Test
    public void testStaticField() {
        test("StaticField");
    }

    @Test
    public void testStaticFieldMultiStores() {
        test("StaticFieldMultiStores");
    }

    // Tests array
    @Test
    public void testArray() {
        test("Array");
    }

    @Test
    public void testArrayField() {
        test("ArrayField");
    }

    @Test
    public void testArrayInter() {
        test("ArrayInter");
    }

    @Test
    public void testArrayInter2() {
        test("ArrayInter2");
    }

    @Test
    public void testArrayLoops() {
        test("ArrayLoops");
    }

    @Test
    public void testArrayCorner() {
        test("ArrayCorner");
    }

    // Other tests
    @Test
    public void testReference() {
        test("Reference");
    }

    @Test
    public void testObjSens() {
        test("ObjSens");
    }

    @Test
    public void testObjSens2() {
        test("ObjSens2");
    }

    @Test
    public void testArrayInField() {
        test("ArrayInField");
    }

    @Test
    public void testMaxPQ() {
        test("MaxPQ");
    }
}
