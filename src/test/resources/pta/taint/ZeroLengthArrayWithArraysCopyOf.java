import java.util.Arrays;

public class ZeroLengthArrayWithArraysCopyOf{

    public static void main(String[] args) {
        testZeroLengthArrayPath();
    }

    public static void testZeroLengthArrayPath() {
        String[] original = new String[0];
        String[] copy = Arrays.copyOf(original, original.length + 1);
        copy[copy.length - 1] = getSourceData();
        sink(copy[copy.length - 1]);
    }

    public static String getSourceData() {
        return "source_data";
    }

    public static void sink(String data) {
        System.out.println("Sink: " + data);
    }

}
