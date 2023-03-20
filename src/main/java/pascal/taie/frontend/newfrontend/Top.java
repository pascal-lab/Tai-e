package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * see JVM spec 4.10.1.2. Verification Type System <br>
 * when push a double / long to stack, first push a Top. <br>
 * [top, double, ...]
 */
enum Top implements Exp {

    Top;

    @Override
    public Type getType() {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<RValue> getUses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }
}
