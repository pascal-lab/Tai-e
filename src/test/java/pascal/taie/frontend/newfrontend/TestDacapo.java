package pascal.taie.frontend.newfrontend;

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.pta.plugin.reflection.LogItem;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.Timer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestDacapo {

    @Test
    public void test2() {
        runDacapo(List.of("eclipse", "xalan"));
    }

    @Test
    public void test3() {
        runDacapo(List.of("antlr"));
    }

    @Test
    public void test4() {
        runDacapo(List.of("bloat"));
    }

    @Test
    public void test5() {
        runDacapo(List.of("chart"));
    }

    @Test
    public void test7() {
        runDacapo(List.of("fop", "pmd"));
    }

    @Test
    public void test8() {
        runDacapo(List.of("luindex", "lusearch"));
    }

    @Test
    public void test9() {
        runDacapo(List.of("hsqldb"));
    }

    private void runDacapo(List<String> items) {
        World.reset();
        String mainClass = "Harness";

        StringBuilder sb = new StringBuilder();
        int ix = 0;
        for (var i : items) {
            sb.append("java-benchmarks/dacapo-2006/").append(i).append(".jar");
            sb.append(File.pathSeparator)
                    .append("java-benchmarks/dacapo-2006/")
                    .append(i)
                    .append("-deps.jar");
            if (ix < items.size()) {
                sb.append(File.pathSeparator);
            }
            ix++;
        }

        String inputClass = items.stream()
                .flatMap(i -> {
                    String log = "java-benchmarks/dacapo-2006/" + i + "-refl.log";
                    return getInputClasses(log).stream();
                })
                .reduce((a, b) -> a + ',' + b)
                .get();

        Runnable newFrontend = () -> {
            Main.buildWorld(
                    "-java", Integer.toString(6),
                    "-cp", sb.toString(),
                    "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder",
                    "--input-classes", inputClass,
                    "--main-class", mainClass
            );

            Timer.runAndCount(() ->
                    World.get()
                            .getClassHierarchy()
                            .allClasses()
                            .forEach(c -> c.getDeclaredMethods().forEach(m -> {
                                if (!m.isAbstract()) {
                                    m.getIR();
                                }
                            })), "Get All IR");
        };

        Timer.runAndCount(newFrontend, "New frontend builds all the classes in Dacapo " + items);

        Printer.printTestRes(true);
    }

    private List<String> getInputClasses(String path) {
        List<String> res = new ArrayList<>();
        LogItem.load(path).forEach(item -> {
            // add target class
            String target = item.target;
            String targetClass;
            if (target.startsWith("<")) {
                targetClass = StringReps.getClassNameOf(target);
            } else {
                targetClass = target;
            }
            if (StringReps.isArrayType(targetClass)) {
                targetClass = StringReps.getBaseTypeNameOf(target);
            }
            if (!PrimitiveType.isPrimitiveType(targetClass)) {
                res.add(targetClass);
            }
        });
        return res;
    }

}
