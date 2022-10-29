package pascal.taie.project;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.config.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestLocate {

    //String singleClassPath = "src/test/resources/world/src.zip";
    String classPath;

    String javaFileToFind;

    String className;

    static class MockOptions extends Options {
        String classPaths;

        public MockOptions(String classPaths) {
            this.classPaths = classPaths;
        }

        @Override
        public String getClassPath() {
            return classPaths;
        }
    }

    Project createProject(String classPath) {
        Options options = new MockOptions(classPath);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
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
        Assert.assertNotNull(project);

        AnalysisFile f = project.locate(className);
        Assert.assertNotNull(f);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            Assert.assertArrayEquals(in.readAllBytes(), f.resource().getContent());
        }
    }

    @Test
    public void testLocateInZip() throws IOException {
        setupTest(testSrcSingle);
        Project project = createProject(classPath);
        Assert.assertNotNull(project);

        AnalysisFile f = project.locate(className);
        Assert.assertNotNull(f);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            Assert.assertArrayEquals(in.readAllBytes(), f.resource().getContent());
        }
    }

    @Test
    public void testLocateFiles() throws IOException {
        setupTest(testSrcMultiple);
        Project project = createProject(classPath);
        Assert.assertNotNull(project);

        List<AnalysisFile> f = project.locateFiles(className);
        Assert.assertEquals(f.size(), 2);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            byte[] expected = in.readAllBytes();
            Assert.assertArrayEquals(expected, f.get(0).resource().getContent());
            Assert.assertArrayEquals(expected, f.get(1).resource().getContent());
        }
    }
}
