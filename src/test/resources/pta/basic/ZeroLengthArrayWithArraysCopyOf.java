import java.util.Arrays;

public class ZeroLengthArrayWithArraysCopyOf {

    public static void main(String[] args) {
        testZeroLengthArrayPath();
    }

    public static void testZeroLengthArrayPath() {
        String[] original = new String[0];
        String[] copy = Arrays.copyOf(original, original.length + 1);
        PTAAssert.sizeEquals(1, copy);
        PTAAssert.notEquals(original, copy);
        String src = getSourceData();
        copy[copy.length - 1] = src;
        String dst = copy[copy.length - 1];
        PTAAssert.equals(dst, src);
    }

    public static String getSourceData() {
        return "source_data";
    }

}
