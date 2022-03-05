import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class TestMap {

    public static void main(String[] args) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("string", new String("new String()"));
        m.put("class", TestMap.class);
        System.out.println(getMap(m, "string"));

        ConcurrentMap<String, Object> cm = new ConcurrentHashMap<String, Object>();
        cm.put("object", new Object());
        cm.put("testmap", new TestMap());
        System.out.println(getConcurrentMap(cm, "object"));
    }

    static Object getMap(Map<String, Object> m, String key) {
        return m.get(key);
    }

    static Object getConcurrentMap(ConcurrentMap<String, Object> m, String key) {
        return m.get(key);
    }
}
