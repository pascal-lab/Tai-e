import java.util.concurrent.ConcurrentHashMap;

public class UnsoundMap {
    public static void main(String[] args) {
        ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>();
        String s = new String("514");
        map.put(114, s);
        PTAAssert.contains(map.get(114), s);
        map.clear();
    }
}
