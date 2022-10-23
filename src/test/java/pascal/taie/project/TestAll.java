package pascal.taie.project;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestAll {

    String classes = "src/test/resources/world/classes.jar";

    String jarInZip = "src/test/resources/world/JarInZipTest.zip";

    String cards = "src/test/resources/world/Cards.class";

    FileContainer loadContainer(String path) throws IOException {
        FileContainer[] c = new FileContainer[1];
        FileLoader loader = FileLoader.get();
        loader.loadFile(Paths.get(path), (a) -> null, a -> {
            c[0] = a; return null;
        });
        return c[0];
    }

    @Test
    public void loadZip() throws IOException {
        FileContainer[] c = new FileContainer[1];
        FileLoader loader = FileLoader.get();
        loader.loadFile(Paths.get(classes), (a) -> null, a -> {
            c[0] = a; return null;
        });

        Assert.assertNotNull(c[0]);
        for (var i : c[0].getFiles()) {
            if (i.getFileName().equals("Cards.class")) {
                Assert.assertArrayEquals(i.getResource().getContent(),
                        Files.readAllBytes(Paths.get(cards)));
                return;
            }
        }
        Assert.fail();
    }

    @Test
    public void testJarInZip() throws IOException {
        FileContainer c = loadContainer(jarInZip);
        Assert.assertNotNull(c);

        byte[] Card1 = null;
        byte[] Card2 = null;

        for (var i : c.getContainers()) {
            if (i.getFileName().equals("a.zip")) {
                for (var z : i.getContainers()) {
                    if (z.getFileName().equals("classes.jar")) {
                        for (var t : z.getFiles()) {
                            if (t.getFileName().equals("Cards.class")) {
                                Card1 = t.getResource().getContent();
                            }
                        }
                    }
                }
            }

            if (i.getFileName().equals("classes.jar")) {
                for (var z : i.getFiles()) {
                    if (z.getFileName().equals("Cards.class")) {
                        Card2 = z.getResource().getContent();
                    }
                }
            }
        }

        Assert.assertNotNull(Card1);
        Assert.assertNotNull(Card2);
        Assert.assertArrayEquals(Card1, Card2);
    }
}
