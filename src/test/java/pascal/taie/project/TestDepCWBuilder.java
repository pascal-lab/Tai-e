package pascal.taie.project;

import org.junit.Test;
import pascal.taie.frontend.newfrontend.ClassInfoCWBuilder;
import pascal.taie.frontend.newfrontend.ClassSource;
import pascal.taie.frontend.newfrontend.DepCWBuilder;

import java.util.List;

public class TestDepCWBuilder {

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

    @Test
    public void testClassInfo() {
        String mainClass = "PrintClassPath";

        Project project = createProject(path, mainClass);
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var w1 = depCWBuilder.getClosedWorld();

        Project project2 = createProject(path, mainClass);
        ClassInfoCWBuilder classInfoCWBuilder = new ClassInfoCWBuilder();
        classInfoCWBuilder.build(project2);
        var w2 = classInfoCWBuilder.getClosedWorld();

        var w1ClassNames = w1.stream().map(ClassSource::getClassName).toList();
        var w2ClassNames = w2.stream().map(ClassSource::getClassName).toList();

        System.out.println("DepCWBuilder exclusive:");
        for (String s : w1ClassNames) {
            if (!w2ClassNames.contains(s)) {
                System.out.println("    " + s);
            }
        }

        System.out.println("------------------------------------------");

        System.out.println("ClassInfoCWBuilder exclusive:");
        for (String s : w2ClassNames) {
            if (!w1ClassNames.contains(s)) {
                System.out.println("    " + s);
            }
        }
    }
}
