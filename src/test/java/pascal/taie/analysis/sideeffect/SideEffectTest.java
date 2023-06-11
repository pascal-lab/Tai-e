package pascal.taie.analysis.sideeffect;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class SideEffectTest {

    private static final String CP = "src/test/resources/sideeffect/";

    private static void test(String dir, String main) {
        Tests.testMain(main, CP + dir, "side-effect",
                "-a", "pta=implicit-entries:false",
                "-a", "cg=algorithm:pta");
    }

    @Test
    public void testStatic() {
        test("static", "StaticStore");
    }

    @Test
    public void testSimple() {
        test("simple", "SimpleCases");
    }

    @Test
    public void testLinkedList() {
        test("linkedlist", "LinkedList");
    }

    @Test
    public void testBubbleSort() {
        test("array", "BubbleSort");
    }

    @Test
    public void testPureArray() {
        test("array", "PureTest");
    }

    @Test
    public void testConstructor() {
        test("constructor", "ConstructorTest");
    }

    @Test
    public void testPrimitive() {
        test("primitive", "PrimitiveTest");
    }

    @Test
    public void testArray() {
        test("array", "Arrays");
    }

    @Test
    public void testSideEffects() {
        test("sideeffects", "SideEffects");
    }

    @Test
    public void testGlobal() {
        test("globals", "Globals");
    }

    @Test
    public void testInheritance() {
        test("inheritance", "Inheritance");
    }

    @Test
    public void testInterProc() {
        test("interproc", "InterProc");
    }

    @Test
    public void testLoops() {
        test("loops", "Loops");
    }

    @Test
    public void testNull() {
        test("null", "Null");
    }

    @Test
    public void testOop(){
        test("oop", "OOP");
    }

    @Test
    public void testMilanova(){
        test("milanova", "Milanova");
    }

    @Test
    public void PolyLoopTest() {
        test("loops", "PolyLoop");
    }

}
