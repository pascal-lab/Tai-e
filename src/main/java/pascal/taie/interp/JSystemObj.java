package pascal.taie.interp;


import pascal.taie.World;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.lang.reflect.Method;
import java.util.List;

public class JSystemObj extends JClassObj {
    static JObject out = new JObject(World.get().getTypeSystem().getClassType(PRINT_STREAM)) {
        @Override
        public JMethod getMethod(String name, List<Type> args, Type retType) {
            if (name.equals("println")) {
                System.out.println();
            }
        }
    };
    final static String SYSTEM = "java.lang.System";
    final static String PRINT_STREAM = "java.io.PrintStream";
    public JSystemObj() {
        super(World.get().getTypeSystem().getClassType(SYSTEM));
    }

    @Override
    public JValue getFields(String name) {
        if ("out".equals(name)) {
            return out;
        }
        throw new IllegalArgumentException("no such fields [ " + name + " ] for " + SYSTEM);
    }
}
