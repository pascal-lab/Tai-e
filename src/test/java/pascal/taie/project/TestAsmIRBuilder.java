package pascal.taie.project;

import org.junit.Test;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.frontend.newfrontend.AsmIRBuilder;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.ClassHierarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestAsmIRBuilder {

    Project createProject(String classPath, String mainClass, List<String> inputClasses) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        options.setInputClasses(inputClasses);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    ClassHierarchy getCh(String mainClass, int javaVersion) {

        String worldPath = "src/test/resources/world";
        String classPath = "java-benchmarks/JREs/jre1." + javaVersion + "/rt.jar";
        String jcePath = "java-benchmarks/JREs/jre1." + javaVersion + "/jce.jar";
        String jssePath = "java-benchmarks/JREs/jre1." + javaVersion + "/jsse.jar";

        List<String> paths = List.of(worldPath, classPath, jcePath, jssePath);
        String path = paths.stream().reduce((i, j) -> i + ";" + j).get();

        // Collections.addAll(args, "-pp");
        // Collections.addAll(args, "-a", "cfg");
        // Collections.addAll(args, "-cp", worldPath);
        // Collections.addAll(args, "-java", Integer.toString(javaVersion));
        // Collections.addAll(args, "-m", "If");
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
    public void testMinimal() {
        var ch = getCh("Minimal", 6);
        ch.allClasses()
                .forEach(i -> { i.getDeclaredMethods().forEach(m -> {
                    JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                    AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                    builder1.build();
            });
        });
    }

    @Test
    public void testAllinOne() {
        List<String> methods = List.of(
                "arrayAccess", "newArray", "assign", "binary", "binaryMixedType",
                "copy", "instanceOf", "cast", "ifStmt", "gotoStmt", "switchStmt", "invoke",
                "returnInt", "exception", "monitor", "iinc");
        var ch = getCh("AllInOne", 6);
        ch.allClasses()
                .filter(i -> i.getSimpleName().equals("AllInOne"))
                .forEach(i -> {
                    i.getDeclaredMethods()
                            .stream().filter(j -> methods.contains(j.getName()))
                            .forEach(m -> {
                                JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                                AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                                builder1.build();
                                System.out.println(m.getName() + " : " + builder1.getIr().getStmts());
                                System.out.println(m.getName() + " : " + builder1.getIr().getExceptionEntries());
                            });
                });
    }

    @Test
    public void testLocalVariableTable() {
        var ch = getCh("CollectionTest", 6);
        ch.allClasses()
                .filter(i -> i.getSimpleName().equals("CollectionTest"))
                .forEach(i -> {
                    i.getDeclaredMethods()
                            .stream().filter(j -> j.getName().equals("p"))
                            .forEach(m -> {
                                JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                                AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                                builder1.build();
                                for (var stmt : builder1.getIr().getStmts()) {
                                    System.out.println(stmt);
                                }
                            });
                });
    }

    @Test
    public void testLambda() {
        var ch = getCh("Lambda", 8);
        ch.allClasses()
                .forEach(i -> {
                    i.getDeclaredMethods()
                            .forEach(m -> {
                                JSRInlinerAdapter jsr = (JSRInlinerAdapter) m.getMethodSource();
                                AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                                builder1.build();
                                // IRPrinter.print(ir, System.out);
                            });
                });
    }
}
