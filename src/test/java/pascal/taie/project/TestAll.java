package pascal.taie.project;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class TestAll {

    String classes = "src/test/resources/world/classes.jar";

    String jarInZip = "src/test/resources/world/JarInZipTest.zip";

    String cards = "src/test/resources/world/Cards.class";

    FileContainer loadContainer(String path) throws IOException {
        FileContainer[] c = new FileContainer[1];
        FileLoader loader = FileLoader.get();
        loader.loadFile(Paths.get(path), null, (a) -> null, a -> {
            c[0] = a;
            return null;
        });
        return c[0];
    }

    @Test
    public void loadZip() throws IOException {
        FileContainer[] c = new FileContainer[1];
        FileLoader loader = FileLoader.get();
        loader.loadFile(Paths.get(classes), null, (a) -> null, a -> {
            c[0] = a;
            return null;
        });

        assertNotNull(c[0]);
        for (var i : c[0].files()) {
            if (i.fileName().equals("Cards.class")) {
                assertSame(i.rootContainer(), c[0]);
                assertArrayEquals(i.resource().getContent(),
                        Files.readAllBytes(Paths.get(cards)));
                return;
            }
        }
        fail();
    }

    @Test
    public void testJarInZip() throws IOException {
        FileContainer c = loadContainer(jarInZip);
        assertNotNull(c);

        byte[] Card1 = null;
        byte[] Card2 = null;

        for (var i : c.containers()) {
            if (i.fileName().equals("a.zip")) {
                for (var z : i.containers()) {
                    if (z.fileName().equals("classes.jar")) {
                        for (var t : z.files()) {
                            assertSame(t.rootContainer(), c);
                            if (t.fileName().equals("Cards.class")) {
                                Card1 = t.resource().getContent();
                            }
                        }
                    }
                }
            }

            if (i.fileName().equals("classes.jar")) {
                for (var z : i.files()) {
                    assertSame(z.rootContainer(), c);
                    if (z.fileName().equals("Cards.class")) {
                        Card2 = z.resource().getContent();
                    }
                }
            }
        }

        assertNotNull(Card1);
        assertNotNull(Card2);
        assertArrayEquals(Card1, Card2);
    }
}
