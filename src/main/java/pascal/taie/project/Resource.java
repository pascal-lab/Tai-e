package pascal.taie.project;

import java.io.IOException;
import java.io.InputStream;

public interface Resource {

    InputStream getInputStream() throws IOException;

    byte[] getContent() throws IOException;

}
