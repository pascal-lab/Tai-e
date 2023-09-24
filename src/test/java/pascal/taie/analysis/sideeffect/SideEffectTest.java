package pascal.taie.analysis.sideeffect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.analysis.Tests;

public class SideEffectTest {

    private static final String CLASS_PATH = "src/test/resources/sideeffect/";

    private static void testSideEffect(String mainClass) {
        Tests.testMain(mainClass, CLASS_PATH, "side-effect",
                "-a", "pta=implicit-entries:false",
                "-a", "cg=algorithm:pta");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "StaticStore",
            "SimpleCases",
            "LinkedList",
            "BubbleSort",
            "PureTest",
            "ConstructorTest",
            "PrimitiveTest",
            "Arrays",
            "SideEffects",
            "Globals",
            "Inheritance",
            "InterProc",
            "Recursion",
            "Loops",
            "Null",
            "OOP",
            "Milanova",
            "PolyLoop"
    })
    void test(String mainClass) {
        testSideEffect(mainClass);
    }
}
