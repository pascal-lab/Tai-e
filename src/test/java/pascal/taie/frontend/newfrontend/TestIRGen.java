package pascal.taie.frontend.newfrontend;

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

public class TestIRGen {

    private static final String path = "src/test/resources/frontend";

    private static final String outputPath = "output";

    private static final String planPath = path + "/" + "plan/plan.yml";

    private static final List<String> willFailed = List.of(
            "Inner1",
            "Inner2");

    private static final List<String> targets = List.of(
            "AddTest",
            "Arith",
            "Arr",
            "ArrayLength",
            "Assignment",
            "BinaryTree",
            "Call1",
            "Cond",
            "Conversion",
            "ExpTest",
            "ForLoop",
            "If",
            "If2",
            "Inner1",
            "Inner2",
            "InstanceOf",
            "Left",
            "Literal",
            "Locate1",
            "Loop",
            "Obj1",
            "PPExp",
            "SameName",
            "StaticCall",
            "Str",
            "Super",
            "SuperInvocation",
            "Switch",
            "Synchronized",
            "Try1",
            "Try2",
            "Try3",
            "Try4",
            "Try5",
            "Try6",
            "Try7",
            "TypeConv",
            "TypeLiteral",
            "Varargs");

    private static void buildWorld(String mainClass) {
        Main.main(new String[]{"-cp", path, "--input-classes", mainClass, "-pp", "-a", "cfg" });
    }

    private void outputIr(String klass, String path) {
        buildWorld(klass);
        JClass mainClass = World.get().getClassHierarchy().getClass(klass);
        try (PrintStream fout = new PrintStream(makePath(path, klass))) {
            mainClass.getDeclaredMethods()
                    .stream()
                    .sorted(Comparator.comparing(JMethod::toString))
                    .forEach(m ->
                            IRPrinter.print(m.getIR(), fout));
            fout.println("------------------------------\n");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        World.reset();
    }

    @Test
    public void testIRBuilder() {
        targets.forEach(klass -> {
            outputIr(klass, outputPath);
//            try {
//                File f1 = new File(makePath(path, klass));
//                File f2 = new File(makePath(outputPath, klass));
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                assert false;
//            }
        });
    }


    private String makePath(String dir, String klass) {
        return dir + "/" + klass + ".tir";
    }

}
