package pascal.taie.project;

import org.junit.Test;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.frontend.newfrontend.AllClassesCWBuilder;
import pascal.taie.frontend.newfrontend.AsmIRBuilder;
import pascal.taie.frontend.newfrontend.AsmMethodSource;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.ClosedWorldBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.ClassHierarchy;

import java.util.ArrayList;
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

    // Note: if javaVersion is less than 7, then assertion about frames will fail
    ClassHierarchy getCh(String mainClass, List<String> otherClasses, int javaVersion) {

        String worldPath = "src/test/resources/world";
        String classPath = "java-benchmarks/JREs/jre1." + javaVersion + "/rt.jar";
        String jcePath = "java-benchmarks/JREs/jre1." + javaVersion + "/jce.jar";
        String jssePath = "java-benchmarks/JREs/jre1." + javaVersion + "/jsse.jar";

        List<String> paths = List.of(worldPath, classPath, jcePath, jssePath);
        String path = paths.stream().reduce((i, j) -> i + ";" + j).get();

        List<String> args = new ArrayList<>();
        // Note: run Tai-e main may produce OutOfMemoryError
//        Collections.addAll(args, "-pp");
//        Collections.addAll(args, "-a", "cfg");
//        Collections.addAll(args, "-cp", worldPath);
//        Collections.addAll(args, "-java", Integer.toString(javaVersion));
//        Collections.addAll(args, "-m", mainClass);
//        Main.main(args.toArray(new String[0]));

        Project project = createProject(path, mainClass, otherClasses);
        ClosedWorldBuilder depCWBuilder = new AllClassesCWBuilder();
        depCWBuilder.build(project);
        var cw = depCWBuilder.getClosedWorld();
        ClassHierarchyBuilder builder = new DefaultCHBuilder();
        return builder.build(cw);
    }

    ClassHierarchy getCh(String mainClass, int javaVersion) {
        return getCh(mainClass, List.of(), javaVersion);
    }

    @Test
    public void testMinimal() {
        var ch = getCh("Minimal", 6);
        ch.allClasses()
                .forEach(i -> { i.getDeclaredMethods().forEach(m -> {
                    AsmMethodSource jsr = (AsmMethodSource) m.getMethodSource();
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
                                AsmMethodSource jsr = (AsmMethodSource) m.getMethodSource();
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
                                AsmMethodSource jsr = (AsmMethodSource) m.getMethodSource();
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
        long startTime = System.currentTimeMillis();
        var ch = getCh("Lambda", 8);
        long endTime = System.currentTimeMillis();

        System.out.println("build ch: " + (endTime - startTime) / 1000.0);

        long startTime2 = System.currentTimeMillis();
        ch.allClasses()
                .forEach(i -> {
                    i.getDeclaredMethods()
                            .forEach(m -> {
                                AsmMethodSource jsr = (AsmMethodSource) m.getMethodSource();
                                AsmIRBuilder builder1 = new AsmIRBuilder(m, jsr);
                                builder1.build();
                                System.out.println(m);
                                if (m.toString().equals("<java.util.Spliterators$ArraySpliterator: java.util.Spliterator trySplit()>")
                                    && builder1.getIr() != null) {
                                    IRPrinter.print(builder1.getIr(), System.out);
                                }
                            });
                });
        long endTime2 = System.currentTimeMillis();

        System.out.println("build IR: " + (endTime2 - startTime2) / 1000.0);
        System.out.println(ch.allClasses().mapToLong(i -> i.getDeclaredMethods().size()).sum());
    }
}
