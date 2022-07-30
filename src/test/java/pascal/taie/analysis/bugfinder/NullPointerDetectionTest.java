package pascal.taie.analysis.bugfinder;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.bugfinder.nullpointer.NullPointerDetection;

public class NullPointerDetectionTest {

    private static final String folderPath = "src/test/resources/bugfinder";

    void testNullPointerException(String inputClass) {
        Tests.test(inputClass, folderPath, NullPointerDetection.ID);
    }

    @Test
    public void test() {
        testNullPointerException("NullDeref");
    }

    @Test
    public void test2() {
        testNullPointerException("NullDeref2");
    }

    @Test
    public void test3() {
        testNullPointerException("NullDeref3");
    }
}
