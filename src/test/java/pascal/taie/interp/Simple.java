package pascal.taie.interp;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;

public class Simple {

    static void init(String mainClass) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/interp", "--main-class", mainClass,
                "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder");
    }

    @Test
    public void test() {
        init("OnePlusOne");
        VM vm = new VM();
        vm.exec();
    }
}
