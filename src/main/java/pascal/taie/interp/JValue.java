package pascal.taie.interp;

import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

public interface JValue {
    public enum ValueType {
        JLiteral,
        JArray,
        JObject
    }

    static ValueType TypeToJValueType(Type t) {
        if (t instanceof PrimitiveType || t instanceof NullType) {
            return ValueType.JLiteral;
        } else if (t instanceof ArrayType) {
            return ValueType.JArray;
        } else {
            return ValueType.JObject;
        }
    }
    Object toJavaObj();

    static int getInt(JValue v) throws IllegalStateException {
        if (v instanceof JLiteral l &&
            l.value instanceof IntLiteral i) {
            return i.getValue();
        } else {
            throw new IllegalStateException(v + " is not int value");
        }
    }

    static JArray getJArray(JValue v) throws IllegalStateException {
        if (v instanceof JArray j) {
            return j;
        } else {
            throw new IllegalStateException(v + " is not array value");
        }
    }

    static JObject getObject(JValue v) {
        if (v instanceof JObject j) {
            return j;
        } else {
            throw new IllegalStateException(v + " is not Object value");
        }
    }

    static JClassObj getClassObj(JValue v) {
        if (v instanceof JClassObj j) {
            return j;
        } else {
            throw new IllegalStateException(v + " is not Class Object value");
        }
    }
}
