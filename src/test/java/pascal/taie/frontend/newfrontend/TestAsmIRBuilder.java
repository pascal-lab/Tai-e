package pascal.taie.frontend.newfrontend;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.config.LoggerConfigs;
import pascal.taie.config.Options;
import pascal.taie.frontend.soot.SootWorldBuilder;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.project.MockOptions;
import pascal.taie.project.OptionsProjectBuilder;
import pascal.taie.project.Project;
import pascal.taie.project.ProjectBuilder;
import pascal.taie.util.ClassNameExtractor;
import pascal.taie.util.Timer;
import soot.Body;
import soot.G;
import soot.Scene;
import soot.SootResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static soot.SootClass.HIERARCHY;

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

        List<String> args = new ArrayList<>();
        // Collections.addAll(args, "-pp");
        // Collections.addAll(args, "-a", "cfg");
        Collections.addAll(args, "-cp", worldPath);
        Collections.addAll(args, "-java", Integer.toString(javaVersion));
        Collections.addAll(args, "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder");
        Collections.addAll(args, "-m", mainClass);
        Main.buildWorld(args.toArray(new String[0]));

//        Project project = createProject(path, mainClass, otherClasses);
//        ClosedWorldBuilder depCWBuilder = new AllClassesCWBuilder();
//        depCWBuilder.build(project);
//        var cw = depCWBuilder.getClosedWorld();
//        ClassHierarchyBuilder builder = new DefaultCHBuilder();
//        return builder.build(cw);
        return World.get().getClassHierarchy();
    }

    void getAllIR(ClassHierarchy classHierarchy) {
        classHierarchy.allClasses()
                .forEach(c -> c.getDeclaredMethods()
                        .forEach(m -> {
                            if (! m.isAbstract()) {
                                m.getIR();
                            }
                        }));
    }

    ClassHierarchy getCh(String mainClass, int javaVersion) {
        String worldPath = "src/test/resources/world";
        return getCh(mainClass, List.of(worldPath + "/ "+ mainClass), javaVersion);
    }

    @Test
    public void testMinimal() {
        var ch = getCh("Minimal", 6);
        getAllIR(ch);
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
        getAllIR(ch);
        long endTime2 = System.currentTimeMillis();

        System.out.println("build IR: " + (endTime2 - startTime2) / 1000.0);
        System.out.println(ch.allClasses().mapToLong(i -> i.getDeclaredMethods().size()).sum());
    }

    @Test
    public void testCornerCase1() {
        String mainClass = "CornerCaseMayBeNotRunnable";
        ClassHierarchy ch = getCh(mainClass, 8);
        JClass c = ch.getClass(mainClass);
        assert c != null;
        for (var m : c.getDeclaredMethods()) {
            IRPrinter.print(m.getIR(), System.out);
        }
    }

    /**
     * Copied from pascal.taie.ir.IRTest.testStmtIndexer()
     */
    @Test
    public void testStmtIndexerForNewFrontend() {
        int javaVersion = 8;
        String worldPath = "src/test/resources/world";

        Main.buildWorld(
                "-java", Integer.toString(javaVersion),
                "-acp", worldPath,
                "--input-classes", "AllInOne",
                "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder",
                "--pre-build-ir"
                );
        World.get()
                .getClassHierarchy()
                .applicationClasses()
                .forEach(c -> {
                    for (JMethod m : c.getDeclaredMethods()) {
                        if (!m.isAbstract()) {
                            IR ir = m.getIR();
//                            IRPrinter.print(ir, System.out);
                            for (Stmt s : ir) {
                                Assert.assertEquals(s, ir.getObject(s.getIndex()));
                            }
                        }
                    }
                });
    }

    @Test
    public void benchmarkForNewFrontEnd() {
        int javaVersion = 8;
        String worldPath = "src/test/resources/world";

        Runnable newFrontend = () -> {
            Main.buildWorld(
                    "-java", Integer.toString(javaVersion),
                    "-acp", jrePaths(javaVersion),
                    "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder",
                    "--pre-build-ir"
            );

            Timer.runAndCount(() ->
            World.get()
                    .getClassHierarchy()
                    .allClasses()
                    .forEach(c -> c.getDeclaredMethods().forEach(m -> {
                        if (!m.isAbstract()) m.getIR();
                    })), "Get All IR");
        };

        Timer.runAndCount(newFrontend, "New frontend builds all the classes in jre1.8");

        AtomicLong stmtCount = new AtomicLong();
        AtomicLong varCount = new AtomicLong();

        World.get()
                .getClassHierarchy()
                .applicationClasses()
                .forEach(c -> {
                    for (JMethod m : c.getDeclaredMethods()) {
                        if (!m.isAbstract()) {
                            IR ir = m.getIR();
                            stmtCount.addAndGet(ir.getStmts().size());
                            varCount.addAndGet(ir.getVars().size());
                        }
                    }
                });

        System.out.println("Count of all the stmts: " + stmtCount.get());
        System.out.println("Count of all the vars: " + varCount.get());
    }

    @Test
    public void benchmarkForSoot() {
        int javaVersion = 8;
        String worldPath = "src/test/resources/world";

        Options options = getOptionsAndAnalysisConfig(
                "-java", Integer.toString(javaVersion),
                "-acp", jrePaths(javaVersion),
                "--pre-build-ir"
        );

        initSoot(options);

        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-cp", jrePaths(javaVersion));
        // process --app-class-path
        String appClassPath = options.getAppClassPath();
        if (appClassPath != null) {
            for (String path : appClassPath.split(File.pathSeparator)) {
                args.addAll(ClassNameExtractor.extract(path));
            }
        }

        Timer.runAndCount(
                () -> runSoot(args.toArray(new String[0])),
                "Soot builds all the classes in jre1.8"
        );

        AtomicLong stmtCount = new AtomicLong();
        AtomicLong varCount = new AtomicLong();

        Scene scene = Scene.v();
        scene.getClasses()
                .forEach(c -> {
                    c.getMethods().forEach(m -> {
                        if (!m.isConcrete()) return;
                        Body body = m.retrieveActiveBody();
                        stmtCount.addAndGet(body.getUnits().size());
                        varCount.addAndGet(body.getLocalCount());
                    });
                });

        System.out.println("Count of all the stmts: " + stmtCount.get());
        System.out.println("Count of all the vars: " + varCount.get());
    }

    @Test
    public void testIllTyping() {
        String worldPath = "src/test/resources/world/multi.jar";

        initSoot(new Options());
        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-cp", worldPath);
        Collections.addAll(args, "-process-dir", worldPath);
        Collections.addAll(args, "-pp");
        Collections.addAll(args, "Multi");

        Timer.runAndCount(() -> runSoot(args.toArray(new String[0])),
                "Try to make soot type inference consume tons of time & memory \n" +
                         "(just build ir for one class with < 30 lines of java source)");

        System.out.println("Let new frontend build this class");

        int javaVersion = 8;
        Main.buildWorld(
                "-java", Integer.toString(javaVersion),
                "-acp", worldPath,
                "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder"
        );

        Timer.runAndCount(() -> {
            for (JMethod m : World.get().getClassHierarchy().getClass("Multi").getDeclaredMethods()) {
                if (m.getName().equals("main")) {
                    IRPrinter.print(m.getIR(), System.out);
                }
            }
        }, "new frontend build");
    }

    private static void initSoot(Options options) {
        // reset Soot
        G.reset();

        // set Soot options
        soot.options.Options.v().set_output_dir(
                new File(options.getOutputDir(), "sootOutput").toString());
        soot.options.Options.v().set_output_format(
                soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_app(true);
        // exclude jdk classes from application classes
        soot.options.Options.v().set_exclude(List.of("jdk.*", "apple.laf.*"));
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_no_writeout_body_releasing(true);
        soot.options.Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        soot.options.Options.v().setPhaseOption("jb", "model-lambdametafactory:false");
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");
        soot.options.Options.v().set_allow_phantom_refs(true); // allow phantom
        soot.options.Options.v().set_drop_bodies_after_load(false); // pre-build-ir
        soot.options.Options.v().set_process_jar_dir(List.of("java-benchmarks/JREs/jre1.8"));

        Scene scene = G.v().soot_Scene();
        addBasicClasses(scene);
    }

    private Options getOptionsAndAnalysisConfig(String... args) {
        LoggerConfigs.reconfigure();
        Options options = Options.parse(args);
        LoggerConfigs.setOutput(options.getOutputDir());
        return options;
    }

    private static void runSoot(String[] args) {
        try {
            soot.Main.v().run(args);
        } catch (SootResolver.SootClassNotFoundException e) {
            throw new RuntimeException(e.getMessage()
                    .replace("is your soot-class-path set",
                            "are your class path and class name given"));
        } catch (AssertionError e) {
            if (e.getStackTrace()[0].toString()
                    .startsWith("soot.SootResolver.resolveClass")) {
                throw new RuntimeException("Exception thrown by class resolver," +
                        " are your class path and class name given properly?", e);
            }
            throw e;
        } catch (Exception e) {
            if (e.getStackTrace()[0].getClassName().startsWith("soot.JastAdd")) {
                throw new RuntimeException("""
                        Soot frontend failed to parse input Java source file(s).
                        This exception may be caused by:
                        1. syntax or semantic errors in the source code. In this case, please fix the errors.
                        2. language features introduced by Java 8+ in the source code.
                           In this case, you could either compile the source code to bytecode (*.class)
                           or rewrite the code by using old features.""", e);
            }
            throw e;
        }
    }

    private static final String BASIC_CLASSES = "basic-classes.yml";
    private static void addBasicClasses(Scene scene) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class);
        try {
            InputStream content = SootWorldBuilder.class
                    .getClassLoader()
                    .getResourceAsStream(BASIC_CLASSES);
            List<String> classNames = mapper.readValue(content, type);
            classNames.forEach(name -> scene.addBasicClass(name, HIERARCHY));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Soot basic classes", e);
        }
    }

    private String jrePaths(int javaVersion) {
        String classPath = "java-benchmarks/JREs/jre1." + javaVersion + "/rt.jar";
        String jcePath = "java-benchmarks/JREs/jre1." + javaVersion + "/jce.jar";
        String jssePath = "java-benchmarks/JREs/jre1." + javaVersion + "/jsse.jar";
        String resourcePath = "java-benchmarks/JREs/jre1." + javaVersion + "/resources.jar";
        String charsetsPath = "java-benchmarks/JREs/jre1." + javaVersion + "/charsets.jar";

        List<String> java8AdditionalPath = List.of(resourcePath, charsetsPath);
        List<String> jrePaths = Stream.of(classPath, jcePath, jssePath).toList();
        if (javaVersion >= 8) {
            jrePaths = new ArrayList<>(jrePaths);
            jrePaths.addAll(java8AdditionalPath);
        }
        return jrePaths.stream().reduce((i, j) -> i + File.pathSeparator + j).get();
    }
}
