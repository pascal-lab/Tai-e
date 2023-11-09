package pascal.taie.project;

import org.junit.Test;
import pascal.taie.frontend.newfrontend.ClassSource;
import pascal.taie.frontend.newfrontend.DepCWBuilder;

import java.util.List;

public class TestDepCWBuilder {

    Project createProject(String classPath, String mainClass) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        options.setJavaVersion(8);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    String worldPath = "src/test/resources/world";
    List<String> paths = List.of(worldPath);
    String path = paths.stream().reduce((i, j) -> i + ";" + j).get();


    @Test
    public void test1() {
        String mainClass = "PrintClassPath";
        Project project = createProject(path, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var w = depCWBuilder.getClosedWorld();
        System.out.println("All Classes:" + w.size());
    }

    @Test
    public void test2() {
        String mainClass = "CollectionTest";
        Project project = createProject(path, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var w = depCWBuilder.getClosedWorld();
        System.out.println("All Classes:" + w.size());
    }
}
