package pascal.taie.language.classes;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.Tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pascal.taie.language.classes.Pattern.parseNamePattern;

public class MatcherTest {

    private static final String CLASS_PATH = "src/test/resources/sigmatcher/";

    private static final String MAIN_CLASS = "com.example.X";

    static void test() {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        Collections.addAll(args, "-cp", CLASS_PATH);
        Collections.addAll(args, "-m", MAIN_CLASS);
        Collections.addAll(args, "-a", "cg=algorithm:cha");
        Main.main(args.toArray(new String[0]));
    }

    @Test
    void testGetMethodFromPattern() {
        test();
    }

    @Test
    void testGetClassFromPattern() {

    }


    @Test
    void testGetFieldFromPattern() {

    }
}
