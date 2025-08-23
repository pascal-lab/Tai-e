package pascal.taie.vm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.util.MultiStringsSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VMTest {

    private static final String CP = "src/test/resources/vm";

    @Test
    void testExceptionDate() {
        Main.buildWorld("-pp", "-cp", CP, "-m", "ExceptionDate");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM(World.get());
        vm.exec();
        String output = outputStream.toString();
        String current = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                .format(Calendar.getInstance().getTime());
        assertEquals(current, output);
    }

    @ParameterizedTest
    @MultiStringsSource({"SwapExample", "0\n1\n"})
    @MultiStringsSource({"SwapExample2", "0\n1\n"})
    @MultiStringsSource({"SwapExample3", "0\n1\n"})
    void testCornerCases(String mainClass, String... expected) {
        Main.buildWorld("-pp", "-cp", CP, "-m", mainClass);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM(World.get());
        vm.exec();
        // Normalize line endings across platforms
        String output = outputStream.toString()
                .replaceAll("\\r\\n|\\r|\\n", "\n");
        assertEquals(expected[0], output);
    }

    @Test
    void testNotRunnable() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        try {
            Main.buildWorld("-pp", "-cp", CP, "-m", "CornerCaseMayBeNotRunnable");
            VM vm = new VM(World.get());
            vm.exec();
        } catch (Exception ignored) {
        }
        String[] lines = outputStream.toString()
                .replaceAll("\\r\\n|\\r|\\n", "\n")
                .split("\n");
        String[] last2lines = Arrays.copyOfRange(lines, lines.length - 2, lines.length);
        assertArrayEquals(new String[] {"11", "6"}, last2lines);
    }
}
