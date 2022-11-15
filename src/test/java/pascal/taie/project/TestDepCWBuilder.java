package pascal.taie.project;

import org.junit.Test;
import pascal.taie.frontend.newfrontend.DepCWBuilder;

public class TestDepCWBuilder {

    Project createProject(String classPath, String mainClass) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    @Test
    public void test1() {
        String classPath = "src/test/resources/world;java-benchmarks/JREs/jre1.8/rt.jar";
        String mainClass = "PrintClassPath";
        Project project = createProject(classPath, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);

        System.out.println("haha");
    }

    @Test
    public void test2() {
        String classPath = "src/test/resources/world;java-benchmarks/JREs/jre1.8/rt.jar";
        String mainClass = "CollectionTest";
        Project project = createProject(classPath, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);

        System.out.println("haha");
    }
}
