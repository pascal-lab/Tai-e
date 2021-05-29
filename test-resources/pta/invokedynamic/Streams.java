import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test method reference together with stream.
 */
public class Streams {

    private String arg;

    Streams(String arg) {
        this.arg = arg;
    }

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        List<Streams> list = argList.stream()
                .map(Streams::new)
                .collect(Collectors.toList());
        Streams st = list.get(0);
        String s = st.arg;
    }
}
