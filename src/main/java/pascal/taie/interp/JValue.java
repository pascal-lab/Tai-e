package pascal.taie.interp;

import pascal.taie.language.type.Type;

public interface JValue {

    Object toJVMObj();

    Type getType();

    static int getInt(JValue v) {
        if (v instanceof JPrimitive l &&
            l.value instanceof Integer i) {
            return i;
        } else {
            throw new InterpreterException(v + " is not int value");
        }
    }

    static long getLong(JValue v) {
        if (v instanceof JPrimitive l &&
                l.value instanceof Long i) {
            return i;
        } else {
            throw new InterpreterException(v + " is not long value");
        }
    }

    static JArray getJArray(JValue v) {
        if (v instanceof JArray j) {
            return j;
        } else {
            throw new InterpreterException(v + " is not array value");
        }
    }

    static JObject getObject(JValue v) {
        if (v instanceof JObject j) {
            return j;
        } else {
            throw new InterpreterException(v + " is not Object value");
        }
    }
}
