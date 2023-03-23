package pascal.taie.project;

import org.junit.Test;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.frontend.newfrontend.AsmIRBuilder;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;
import pascal.taie.language.classes.ClassHierarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestAsmIRBuilder {
    String worldPath = "src/test/resources/world";
    String classPath = "java-benchmarks/JREs/jre1.6/rt.jar";
    String jcePath = "java-benchmarks/JREs/jre1.6/jce.jar";
    String jssePath = "java-benchmarks/JREs/jre1.6/jsse.jar";

    List<String> paths = List.of(worldPath, classPath, jcePath, jssePath);
    String path = paths.stream().reduce((i, j) -> i + ";" + j).get();

    Project createProject(String classPath, String mainClass, List<String> inputClasses) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        options.setInputClasses(inputClasses);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    ClassHierarchy getCh(String mainClass) {
        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-pp");
        Collections.addAll(args, "-a", "cfg");
        Collections.addAll(args, "-cp", worldPath);
        Collections.addAll(args, "-java", "6");
        Collections.addAll(args, "-m", "If");
        // Note: run Tai-e main may produce OutOfMemoryError
        // Main.main(args.toArray(new String[0]));
        Project project = createProject(path, mainClass, List.of());
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var cw = depCWBuilder.getClosedWorld();
        ClassHierarchyBuilder builder = new DefaultCHBuilder();
        return builder.build(cw);
    }

    @Test
    public void testIf() {
        var ch = getCh("If");
        int[] total = { 0 };
        int[] zeroBlock = { 0 };
        List<String> targets = new ArrayList<>();
        ch.allClasses().forEach(i -> {
            i.getDeclaredMethods().forEach(m -> {
                JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                builder1.build();
                var cfg = builder1.getLabel2Block();
                System.out.println("[" + m.getSignature() + "]: " + cfg.keySet().size());
                total[0]++;
                if (cfg.keySet().size() == 0) {
                    zeroBlock[0]++;
                    if (! m.isNative() && ! i.isInterface() && !m.isAbstract()) {
                        targets.add(m.getSignature());
                    }
                }
            });
        });
        System.out.println("total: " + total[0] + ", zero:" + zeroBlock[0]);
        assert targets.isEmpty();
    }

    @Test
    public void testMinimal() {
        var ch = getCh("Minimal");
        ch.allClasses()
                .filter(i -> i.getSimpleName().equals("Minimal"))
                .forEach(i -> {
                    i.getDeclaredMethods()
                            .stream().filter(j -> j.getName().equals("f"))
                            .forEach(m -> {
                    JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                    AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                    builder1.build();
                    builder1.buildIR();
                    System.out.println(builder1.getIr().getStmts());
            });
        });
    }

    @Test
    public void testAllinOne() {
        List<String> methods = List.of("arrayAccess", "newArray", "assign",
                "binary", "binaryMixedType", "copy", "instanceOf",
                "cast", "ifStmt", "gotoStmt", "switchStmt", "invoke");
        var ch = getCh("AllInOne");
        ch.allClasses()
                .filter(i -> i.getSimpleName().equals("AllInOne"))
                .forEach(i -> {
                    i.getDeclaredMethods()
                            .stream().filter(j -> methods.contains(j.getName()))
                            .forEach(m -> {
                                JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                                AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                                builder1.build();
                                builder1.buildIR();
                                System.out.println(m.getName() + " : " + builder1.getIr().getStmts());
                            });
                });
    }
}
