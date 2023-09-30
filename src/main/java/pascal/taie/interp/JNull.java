package pascal.taie.interp;

import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.language.type.Type;

public enum JNull implements JValue {
    NULL;

    @Override
    public Type getType() {
        return NullLiteral.get().getType();
    }

    @Override
    public Object toJVMObj() {
        return null;
    }
}
