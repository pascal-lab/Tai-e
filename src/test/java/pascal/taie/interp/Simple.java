package pascal.taie.interp;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;

public class Simple {

    static void init(String mainClass) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/interp", "--input-classes", mainClass,
                "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder");
        World world = World.get();
        world.setMainMethod(world.getClassHierarchy().getClass(mainClass).getDeclaredMethod("main"));
    }

    @Test
    public void test() {
        init("OnePlusOne");
        VM vm = new VM();
        vm.exec();
    }
}
