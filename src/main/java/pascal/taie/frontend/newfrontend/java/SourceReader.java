package pascal.taie.frontend.newfrontend.java;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SourceReader {
    private static final Logger logger = LogManager.getLogger(SourceReader.class);
    public static Optional<char[]> readJavaSourceFile(String path) {
        try (FileInputStream stream = new FileInputStream(path)) {
            byte[] bytes = stream.readAllBytes();
            String temp = new String(bytes, StandardCharsets.UTF_8);
            return Optional.of(temp.toCharArray());
        }  catch (UnsupportedEncodingException e) {
            logger.error("UTF-8 is not supported, inner:" + e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            logger.error("IOException Encounter, inner:" + e.getMessage());
            return Optional.empty();
        }
    }
}
