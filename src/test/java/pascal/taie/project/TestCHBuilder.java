package pascal.taie.project;

import org.junit.Test;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;

import java.util.List;

public class TestCHBuilder {
    Project createProject(String classPath, String mainClass) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    String worldPath = "src/test/resources/world";
    String classPath = "java-benchmarks/JREs/jre1.8/rt.jar";
    String jcePath = "java-benchmarks/JREs/jre1.8/jce.jar";
    String jssePath = "java-benchmarks/JREs/jre1.8/jsse.jar";

    List<String> paths = List.of(worldPath, classPath, jcePath, jssePath);
    String path = paths.stream().reduce((i, j) -> i + ";" + j).get();


    @Test
    public void test1() {
        String mainClass = "PrintClassPath";
        Project project = createProject(path, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var w = depCWBuilder.getClosedWorld();
        ClassHierarchyBuilder builder = new DefaultCHBuilder();
        var ch = builder.build(w);
    }
}
