package pascal.taie.analysis.bugfinder;

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.bugfinder.dataflow.IsNullAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IsNullTest {

    String folderPath = "src/test/resources/bugfinder";

    void testIsNullValue(String inputClass) {
        Tests.test(inputClass, folderPath,
            IsNullAnalysis.ID);
    }

    @Test
    public void test() {
        testIsNullValue("NullDeref");
    }

    @Test
    public void test2() {
        testIsNullValue("NullDeref2");
    }

    @Test
    public void testAnnotation() {
        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-cp", folderPath + ";" + folderPath + "/jsr305-3.0.2.jar");
        Collections.addAll(args, "-m", "NullAnnotation");
        Collections.addAll(args, "-a", IsNullAnalysis.ID);
        Main.main(args.toArray(new String[0]));
//        Tests.test("NullAnnotation", folderPath + ";" + folderPath + "/jsr305-3.0.2.jar", IsNullAnalysis.ID);
    }
}
