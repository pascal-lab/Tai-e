package pascal.taie.project;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestAll {

    String classes = "src/test/resources/world/classes.jar";

    String cards = "src/test/resources/world/Cards.class";

    @Test
    public void loadZip() throws IOException {
        FileContainer[] c = new FileContainer[1];
        FileLoader loader = FileLoader.get();
        loader.loadFile(Paths.get(classes), (a) -> null, a -> {
            c[0] = a; return null;
        });

        Assert.assertNotNull(c[0]);
        for (var i : c[0].getFiles()) {
            if (i.getFileName().equals("Cards")) {
                Assert.assertArrayEquals(i.getResource().getContent(),
                        Files.readAllBytes(Paths.get(cards)));
                return;
            }
        }
        Assert.fail();
    }
}
