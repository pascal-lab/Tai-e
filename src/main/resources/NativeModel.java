public class NativeModel {

    public static void java_lang_System_arraycopy(
            Object src,  int  srcPos,
            Object dest, int destPos, int length) {
        Object[] srcArr = (Object[]) src;
        Object[] destArr = (Object[]) dest;
        for (int i = 0; i < length; ++i) {
            destArr[destPos + i] = srcArr[srcPos + i];
        }
    }
}
