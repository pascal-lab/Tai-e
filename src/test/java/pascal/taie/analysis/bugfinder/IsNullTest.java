package pascal.taie.analysis.bugfinder;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.bugfinder.nullpointer.IsNullAnalysis;

public class IsNullTest {

    private static final String folderPath = "src/test/resources/bugfinder";

    void testIsNullValue(String inputClass) {
        Tests.testInput(inputClass, folderPath, IsNullAnalysis.ID);
    }

    @Test
    public void test() {
        testIsNullValue("NullDeref");
    }

    @Test
    public void test2() {
        testIsNullValue("NullDeref2");
    }
}
