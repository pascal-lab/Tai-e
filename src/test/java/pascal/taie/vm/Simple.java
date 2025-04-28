package pascal.taie.vm;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;

public class Simple {

    static void init(String mainClass) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/interp", "--main-class", mainClass);
    }

    @Test
    public void test() {
        init("OnePlusOne");
        VM vm = new VM(World.get());
        vm.exec();
    }
}
