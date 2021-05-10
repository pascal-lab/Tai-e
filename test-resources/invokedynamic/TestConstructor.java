import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestConstructor {

    private String arg;

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        List<TestConstructor> list = argList.stream()
                .map(TestConstructor::new)
                .collect(Collectors.toList());
        TestConstructor tc = list.get(0);
        String s = tc.arg;
    }
    TestConstructor(String arg) {
        this.arg = arg;
    }
}
