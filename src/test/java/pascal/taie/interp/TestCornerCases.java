package pascal.taie.interp;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class TestCornerCases {

    static void init(String mainClass) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/interp", "--main-class", mainClass
               ,"--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder"
        );
        IRDumper dumper = new IRDumper(AnalysisConfig.of(IRDumper.ID));
        dumper.analyze(World.get().getMainMethod().getDeclaringClass());
    }

    @Test
    public void testSwap() {
        init("SwapExample");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM();
        vm.exec();
        Assert.assertEquals("0\n1\n", outputStream.toString());
    }

    @Test
    public void testSwap2() {
        init("SwapExample2");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM();
        vm.exec();
        Assert.assertEquals("0\n1\n", outputStream.toString());
    }

    @Test
    public void testSwap3() {
        init("SwapExample3");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM();
        vm.exec();
        Assert.assertEquals("0\n1\n", outputStream.toString());
    }

    @Test
    public void testCornerCase1() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        try {
            init("CornerCaseMayBeNotRunnable");
            VM vm = new VM();
            vm.exec();
        } catch (Exception ignored) {
        }
        String[] lines = outputStream.toString()
                .split("\n");
        String[] last2lines = Arrays.copyOfRange(lines, lines.length - 2, lines.length);
        Assert.assertArrayEquals(new String[] { "11", "6" }, last2lines);
    }
}
