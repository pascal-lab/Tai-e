package pascal.taie.project;

import org.junit.Test;
import pascal.taie.analysis.pta.plugin.reflection.LogItem;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.ClosedWorldBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.PrimitiveType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestCHBuilder {
    Project createProject(int javaVersion, String classPath, String mainClass, List<String> inputClasses) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        options.setInputClasses(inputClasses);
        options.setJavaVersion(javaVersion);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    Project createProject(int javaVersion, String classPath, String mainClass) {
        return createProject(javaVersion, classPath, mainClass, List.of());
    }

    String worldPath = "src/test/resources/world";
    String classPath = "java-benchmarks/JREs/jre1.6/rt.jar";
    String jcePath = "java-benchmarks/JREs/jre1.6/jce.jar";
    String jssePath = "java-benchmarks/JREs/jre1.6/jsse.jar";

    List<String> paths = List.of(worldPath, classPath, jcePath, jssePath);
    String path = paths.stream().reduce((i, j) -> i + ";" + j).get();

    @Test
    public void test1() {
        String mainClass = "PrintClassPath";
        Project project = createProject(8, worldPath, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var w = depCWBuilder.getClosedWorld();
        ClassHierarchyBuilder builder = new DefaultCHBuilder();
        var ch = builder.build(w);
        System.out.println(ch.allClasses().count());
    }

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

    private void runDacapo(List<String> items) {
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

        List<String> inputClass = items.stream()
                .flatMap(i -> {
                    String log = "java-benchmarks/dacapo-2006/" + i + "-refl.log";
                    return getInputClasses(log).stream();
                })
                .toList();

        Project project = createProject(6 ,sb.toString(), mainClass, inputClass);
        ClosedWorldBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var w = depCWBuilder.getClosedWorld();
        ClassHierarchyBuilder builder = new DefaultCHBuilder();
        var ch = builder.build(w);
        System.out.println("total for " +
                items.stream().reduce((i, j) -> i + ", " + j).get()
                + ": "
                + ch.allClasses().count());
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
