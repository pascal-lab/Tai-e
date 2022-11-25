package pascal.taie.project;

import org.junit.Test;
import pascal.taie.analysis.pta.plugin.reflection.LogItem;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.ClosedWorldBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.PrimitiveType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestCHBuilder {
    Project createProject(String classPath, String mainClass, List<String> inputClasses) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        options.setInputClasses(inputClasses);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    Project createProject(String classPath, String mainClass) {
        return createProject(classPath, mainClass, List.of());
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
        Project project = createProject(worldPath + ";" + getJRE(8), mainClass);
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

        StringBuilder sb = new StringBuilder(getJRE(6));
        for (var i : items) {
            sb.append(";java-benchmarks/dacapo-2006/").append(i).append(".jar");
            sb.append(";java-benchmarks/dacapo-2006/").append(i).append("-deps.jar");
        }

        List<String> inputClass = items.stream()
                .flatMap(i -> {
                    String log = "java-benchmarks/dacapo-2006/" + i + "-refl.log";
                    return getInputClasses(log).stream();
                })
                .toList();

        Project project = createProject(sb.toString(), mainClass, inputClass);
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

    private String getJRE(int version) {
        String rtPath = "java-benchmarks/JREs/jre1." + version + "/rt.jar";
        String jcePath = "java-benchmarks/JREs/jre1." + version + "/jce.jar";
        String jssePath = "java-benchmarks/JREs/jre1." + version + "/jsse.jar";
        return Stream.of(rtPath, jcePath, jssePath)
                .reduce((a, b) -> a + ";" + b).get();
    }
}
