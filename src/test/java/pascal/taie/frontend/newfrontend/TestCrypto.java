package pascal.taie.frontend.newfrontend;

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.Timer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TestCrypto {

    public static final String CRYPTO_BENCHMARKS_DIR = "crypto-benchmarks/";

    @Test
    public void test_aliyun_oss() throws IOException {
        test("aliyun-oss-java-sdk", true, true);
    }

    @Test
    public void test_biglybt_core() throws IOException {
        test("biglybt-core", true, true);
    }

    @Test
    public void test_bt() throws IOException {
        test("aliyun-oss-java-sdk", true, true);
    }

    @Test
    public void test_dubbo3() throws IOException {
        test("dubbo3", true, true);
    }

    @Test
    public void testFastBoot() throws IOException  {
        test("fast-boot-weixin", true, true);
    }

    @Test
    public void test_game_server() throws IOException {
        test("game-server", true, true);
    }

    @Test
    public void test_gruul() throws IOException {
        test("gruul", true, true);
    }

    @Test
    public void test_ha_bridge() throws IOException {
        test("ha-bridge", true, true);
    }

    @Test
    public void test_hsweb_framework() throws IOException {
        test("hsweb-framework", true, true);
    }

    @Test
    public void test_ijpay() throws IOException {
        test("ijpay", true, true);
    }

    @Test
    public void test_instagram4j() throws IOException {
        test("instagram4j", true, true);
    }

    @Test
    public void test_j360_dubbo_app_all() throws IOException {
        test("j360-dubbo-app-all", true, true);
    }

    @Test
    public void test_mpush() throws IOException {
        test("mpush", true, true);
    }

    @Test
    public void test_my_blog() throws IOException {
        test("my-blog", true, true);
    }

    @Test
    public void test_nettygameserver() throws IOException {
        test("nettygameserver", true, true);
    }

    @Test
    public void test_protools() throws IOException {
        test("protools", true, true);
    }

    @Test
    public void test_public_cms() throws IOException {
        test("public-cms", true, true);
    }

    @Test
    public void test_saturn_console_api() throws IOException {
        test("saturn-console-api", true, true);
    }

    @Test
    public void test_saturn_console_core() throws IOException {
        test("saturn-console-core", true, true);
    }

    @Test
    public void test_smart() throws IOException {
        test("smart", true, true);
    }

    @Test
    public void test_spring_boot_quick() throws IOException {
        test("spring-boot-quick", true, true);
    }

    @Test
    public void test_spring_boot_student() throws IOException {
        test("spring-boot-student", true, true);
    }

    @Test
    public void test_symmmetric_ds() throws IOException {
        test("symmetric-ds", true, true);
    }

    @Test
    public void test_telegram_server() throws IOException {
        test("telegram-server", true, true);
    }

    @Test
    public void test_zheng() throws IOException {
        test("zheng", true, true);
    }

    private void test(String testName, boolean allowPhantom, boolean preBuildIR) throws IOException  {
        String testPath = CRYPTO_BENCHMARKS_DIR + testName;
        String appClassPath = testPath + "/original-classes.jar";
        var dependencies = listRootContainers(testPath + "/dependencies");

        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-java", "8");
        if (allowPhantom) {
            Collections.addAll(args, "-ap");
        }
        if (preBuildIR) {
            Collections.addAll(args, "--pre-build-ir");
        }
        Collections.addAll(args, "-acp", appClassPath);
        Collections.addAll(args, "-cp", String.join(File.pathSeparator, dependencies));
        Collections.addAll(args, "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder");
        String[] argsArr = args.toArray(new String[0]);

        Timer.runAndCount(() -> {
            Main.buildWorld(argsArr);
            if (!preBuildIR) {
                Timer.runAndCount(() ->
                        World.get()
                                .getClassHierarchy()
                                .allClasses()
                                .forEach(c -> c.getDeclaredMethods().forEach(m -> {
                                    if (!m.isAbstract()) {
                                        m.getIR();
                                    }
                                })), "Get All IR because no --pre-build-ir");
            }
        }, "Build world for " + testName + (preBuildIR ? " (with --pre-build-ir)":""));

        Printer.printTestRes(false);

        // To drive the generation of IR of the phantom methods
        World.get()
                .getClassHierarchy()
                .allClasses()
                .forEach(c -> c.getDeclaredMethods().forEach(m -> {
                            if (!m.isAbstract()) {
                                IR ir = m.getIR();
                                ir.getStmts().stream()
                                        .filter(stmt -> stmt instanceof Invoke invoke && !(invoke.getInvokeExp() instanceof InvokeDynamic))
                                        .forEach(stmt -> World.get().getClassHierarchy().resolveMethod(((Invoke) stmt).getMethodRef()));
                            }
                }));

        var phantomCount = World.get()
                .getClassHierarchy()
                .allClasses()
                .filter(JClass::isPhantom).count();
        System.out.println(phantomCount);
        System.out.println(World.get().getClassHierarchy().allClasses().count());
        World.get()
                .getClassHierarchy()
                .allClasses()
                .filter(JClass::isPhantom)
                .forEach(c -> c.getPhantomMethods().forEach(m -> {
                    if (!m.isAbstract()) {
                        IRPrinter.print(m.getIR(), System.out);
                    }
                }));
    }

    public static List<String> listRootContainers(String dir) throws IOException {
        List<String> res = new ArrayList<>();
        Path path = Path.of(dir);

        try (var l = Files.list(path)) {
            for (var subPath : l.toList()) {
                if (subPath.toFile().isDirectory()) {
                    res.addAll(listRootContainers(subPath.toString()));
                }
                else if (subPath.toString().endsWith(".jar") || subPath.toString().endsWith(".war")) {
                    res.add(subPath.toString());
                }
            }
        }

        return res;
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
