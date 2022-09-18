package pascal.taie.analysis.bugfinder;

import org.junit.Test;
import pascal.taie.World;
import pascal.taie.analysis.Tests;
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class CloneIdiomTest {

    private void test(String inputClass) {
        Tests.testInput(inputClass, "src/test/resources/bugfinder/CloneIdiom", CloneIdiom.ID);
        Set<BugInstance> bugInstances = Sets.newSet();
        World.get()
                .getClassHierarchy()
                .applicationClasses()
                .forEach(jClass -> bugInstances.addAll(jClass.getResult(CloneIdiom.ID)));
    }

    @Test
    public void test1() {
        test("CloneIdiom1");
    }

    @Test
    public void test2() {
        test("CloneIdiom2");
    }

    @Test
    public void test3() {
        test("CloneIdiom3");
    }

    @Test
    public void test4() {
        test("CloneIdiom4");
    }
}
