package pascal.taie.interp;

import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

public class JPrimitive implements JValue {

    public final Object value;

    public final PrimitiveType type;

    public JPrimitive(Object v) {
        assert v instanceof Integer ||
                v instanceof Long ||
                v instanceof Float ||
                v instanceof Double;

        this.value = v;

        if (value instanceof Integer) {
            type = PrimitiveType.INT;
        } else if (value instanceof Long) {
            type = PrimitiveType.LONG;
        } else if (value instanceof Float) {
            type = PrimitiveType.FLOAT;
        } else {
            type = PrimitiveType.DOUBLE;
        }
    }

    public static JPrimitive get(Object value) {
        return new JPrimitive(value);
    }

    public JPrimitive getNegValue() {
        if (value instanceof Integer i) {
            return get(-i);
        } else if (value instanceof Long l) {
            return get(-l);
        } else if (value instanceof Float f) {
            return get(-f);
        } else if (value instanceof Double d) {
            return get(-d);
        } else {
            throw new InterpreterException();
        }
    }

    public static JPrimitive getBoolean(boolean b) {
        return new JPrimitive(Utils.toInt(b));
    }

    public static JPrimitive getDefault(PrimitiveType t) {
        return switch (t) {
            case INT, BOOLEAN, BYTE, CHAR, SHORT -> JPrimitive.get(0);
            case LONG -> JPrimitive.get(0L);
            case FLOAT -> JPrimitive.get(0f);
            case DOUBLE -> JPrimitive.get(0d);
        };
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Object toJVMObj() {
        return value;
    }

    @Override
    public Type getType() {
        return type;
    }
}
