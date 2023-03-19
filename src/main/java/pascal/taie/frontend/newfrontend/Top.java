package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.language.type.Type;

/**
 * see JVM spec 4.10.1.2. Verification Type System <br>
 * when push a double / long to stack, first push a Top. <br>
 * [top, double, ...]
 */
class Top implements Exp {
    @Override
    public Type getType() {
        throw new IllegalStateException();
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new IllegalStateException();
    }

    private static final Top instance = new Top();

    private Top() {}

    public static Top getInstance() {
        return instance;
    }
}
