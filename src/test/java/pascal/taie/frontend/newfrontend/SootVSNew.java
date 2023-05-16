package pascal.taie.frontend.newfrontend;

import org.junit.Assert;
import org.junit.Test;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.DepCWBuilder;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.project.MockOptions;
import pascal.taie.project.OptionsProjectBuilder;
import pascal.taie.project.Project;
import pascal.taie.project.ProjectBuilder;
import pascal.taie.util.collection.Maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class SootVSNew {
    Project createProject(String classPath, String mainClass, List<String> inputClasses) {
        MockOptions options = new MockOptions();
        options.setClasspath(classPath);
        options.setMainClass(mainClass);
        options.setInputClasses(inputClasses);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    @Test
    public void f() throws IOException {
        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-a", "cfg");
        Collections.addAll(args, "-cp", worldPath);
        Collections.addAll(args, "-java", "8");
        Collections.addAll(args, "-m", "CloneTest");
        Main.main(args.toArray(new String[0]));
        World w = World.get();

        long startTime2 = System.currentTimeMillis();
        w.getClassHierarchy().allClasses().forEach(i -> {
            if (i.getName().equals("CloneTest")) {
                i.getDeclaredMethods().forEach(j -> {
                    if (!j.isAbstract()) {
                        j.getIR();
                        IRPrinter.print(j.getIR(), System.out);
                    }
                });
            }
        });
        long endTime2 = System.currentTimeMillis();
        System.out.println("build IR: " + (endTime2 - startTime2) / 1000.0);
        System.out.println(w.getClassHierarchy().allClasses().mapToLong(i -> i.getDeclaredMethods().size()).sum());
        // ours(w);
    }

    private void ours(World w) throws FileNotFoundException {
        String mainClass = "PrintClassPath";
        Project project = createProject(path, mainClass, List.of());
        DepCWBuilder depCWBuilder = new DepCWBuilder();
        depCWBuilder.build(project);
        var cw = depCWBuilder.getClosedWorld();
        ClassHierarchyBuilder builder = new DefaultCHBuilder();
        var ch = builder.build(cw);

        var soot = getAllClass(w.getClassHierarchy());
        soot.removeAll(ch.allClasses().map(i -> i.getName()).toList());
        try (PrintWriter writer = new PrintWriter(new File(outPut))) {
            processRes(writer, soot);
        }
    }

    void processRes(PrintWriter writer, Set<String> res) {
        writer.println(res.size());
        writer.println();

        Map<String, Integer> m = Maps.newConcurrentMap();
        for (var i : res) {
            var now = take2(i);
            m.computeIfAbsent(now, v -> 0);
            m.computeIfPresent(now, (k, v) -> v + 1);
        }

        for (var i : m.entrySet().stream().sorted(new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                    Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        }).toList()) {
            writer.println(i.getKey() + " : " + i.getValue());
        }

        writer.println();

        for (var i : res) {
            writer.println(i);
        }
    }

    String take2(String s) {
        int idx1 = s.indexOf('.');
        if (idx1 == -1) {
            return s;
        }
        int idx2 = s.indexOf('.', idx1 + 1);
        if (idx2 == -1) {
            return s;
        } else {
            return s.substring(0, idx2);
        }
    }

    Set<String> getAllClass(ClassHierarchy ch) {
        return ch.allClasses()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
    }

    String worldPath = "src/test/resources/world";
    String classPath = "java-benchmarks/JREs/jre1.6/rt.jar";
    String jcePath = "java-benchmarks/JREs/jre1.6/jce.jar";
    String jssePath = "java-benchmarks/JREs/jre1.6/jsse.jar";

    List<String> paths = List.of(worldPath, classPath, jcePath, jssePath);
    String path = paths.stream().reduce((i, j) -> i + ";" + j).get();

    String outPut = "output/SootNewDiff.txt";
}
