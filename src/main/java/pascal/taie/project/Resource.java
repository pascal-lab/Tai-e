package pascal.taie.project;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface Resource {

    InputStream getInputStream() throws IOException;

    byte[] getContent() throws IOException;

    Path getPath();
}
