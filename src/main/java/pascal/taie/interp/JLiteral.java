package pascal.taie.interp;

import pascal.taie.ir.exp.*;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.collection.Maps;
import polyglot.ast.Do;

import java.util.Map;

public class JLiteral implements JValue {
    public final Literal value;

    public JLiteral(Literal v) {
        this.value = v;
    }

    private static final Map<Literal, JLiteral> cache = Maps.newMap();

    public static JLiteral get(Literal value) {
        return cache.computeIfAbsent(value, JLiteral::new);
    }

    public static JLiteral getDefault(PrimitiveType t) {
        return switch (t) {
            case INT, BOOLEAN, BYTE, CHAR, SHORT -> JLiteral.get(IntLiteral.get(0));
            case LONG -> JLiteral.get(LongLiteral.get(0));
            case FLOAT -> JLiteral.get(FloatLiteral.get(0));
            case DOUBLE -> JLiteral.get(DoubleLiteral.get(0));
        };
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Object toJavaObj() {
        if (value instanceof IntLiteral t) {
            return t.getValue();
        } else if (value instanceof FloatLiteral f){
            return f.getValue();
        } else if (value instanceof DoubleLiteral d) {
            return d.getValue();
        } else if (value instanceof NullLiteral) {
            return null;
        }
        return this;
    }
}
