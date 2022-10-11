package pascal.taie.interp;

import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;

public class JObject implements JValue {

    private final ClassType type;

    private final Map<String, JValue> fields;


    public JObject(ClassType type) {
        this.type = type;
        fields = Maps.newMap();
    }

    public void setFields(String name, JValue value) {
        fields.put(name, value);
    }

    public ClassType getType() {
        return type;
    }

    public JValue getFields(String name) {
        return fields.computeIfAbsent(name, n -> {
            JField f = type.getJClass().getDeclaredField(n);
            if (f == null) {
                throw new IllegalStateException(n + " of " + type + " not exist");
            } else {
                if (f.getType() instanceof PrimitiveType t) {
                    return JLiteral.getDefault(t);
                } else {
                    return null;
                }
            }
        });
    }

    public JMethod getMethod(String name, List<Type> args, Type retType) {
        return type.getJClass().getDeclaredMethod(Subsignature.get(name, args, retType));
    }

    public JMethod getMethod(Subsignature subsignature) {
        return type.getJClass().getDeclaredMethod(subsignature);
    }

    @Override
    public String toString() {
        return "JObject " + ": [" + type + "]";
    }

    @Override
    public Object toJavaObj() {
        return this;
    }
}
