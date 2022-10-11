package pascal.taie.interp;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;

public class Simple {

    static void init(String mainClass) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/interp", "--input-classes", mainClass);
        World world = World.get();
        world.setMainMethod(world.getClassHierarchy().getClass(mainClass).getDeclaredMethod("main"));
    }

    @Test
    public void test() {
        init("OnePlusOne");
        VM vm = new VM();
        vm.exec();
        Assert.assertEquals(1, 1);
    }
}
