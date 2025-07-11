package pascal.taie.project;

import org.junit.jupiter.api.Test;
import pascal.taie.config.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLocate {

    //String singleClassPath = "src/test/resources/world/src.zip";
    String classPath;

    String javaFileToFind;

    String className;

    Project createProject(String classPath) {
        ProjectBuilder builder = new OptionsProjectBuilder(null);
        return builder.build();
    }

    private void setupTest(String[] configs) {
        classPath = configs[0];
        javaFileToFind = configs[1];
        className = configs[2];
    }

    String[] testAll = {"src/test/java", "src/test/java/pascal/taie/project/TestAll.java", "pascal.taie.project.TestAll"};

    String[] testSrcSingle = {
            "src/test/resources/world/java.sql.zip",
            "src/test/resources/world/Driver.java",
            "java.sql.Driver"
    };

    String[] testSrcMultiple = {
            "src/test/resources/world/java.sql.zip;src/test/resources/world/java.sql2.zip",
            "src/test/resources/world/Driver.java",
            "java.sql.Driver"
    };
    @Test
    public void testLocate1() throws IOException {
        setupTest(testAll);
        Project project = createProject(classPath);
        assertNotNull(project);

        AnalysisFile f = project.locate(className);
        assertNotNull(f);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            assertArrayEquals(in.readAllBytes(), f.resource().getContent());
        }
    }

    @Test
    public void testLocateInZip() throws IOException {
        setupTest(testSrcSingle);
        Project project = createProject(classPath);
        assertNotNull(project);

        AnalysisFile f = project.locate(className);
        assertNotNull(f);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            assertArrayEquals(in.readAllBytes(), f.resource().getContent());
        }
    }

    @Test
    public void testLocateFiles() throws IOException {
        setupTest(testSrcMultiple);
        Project project = createProject(classPath);
        assertNotNull(project);

        List<AnalysisFile> f = project.locateFiles(className);
        assertEquals(f.size(), 2);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            byte[] expected = in.readAllBytes();
            assertArrayEquals(expected, f.get(0).resource().getContent());
            assertArrayEquals(expected, f.get(1).resource().getContent());
        }
    }
}
