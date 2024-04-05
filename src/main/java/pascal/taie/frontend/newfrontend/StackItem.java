package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;

import java.util.Objects;

final class StackItem {
    private Exp e;
    private final Exp originalExp;
    private final AbstractInsnNode origin;

    StackItem(Exp e, AbstractInsnNode origin) {
        this.e = e;
        this.originalExp = e;
        this.origin = origin;
    }

    Exp e() {
        return e;
    }

    Exp originalExp() {
        return originalExp;
    }

    Var var() {
        assert e instanceof Var;
        return (Var) e;
    }

    void lift(Var v) {
        this.e = v;
    }

    AbstractInsnNode origin() {
        return origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StackItem) obj;
        return Objects.equals(this.e, that.e) &&
                Objects.equals(this.origin, that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(e, origin);
    }

    @Override
    public String toString() {
        return "StackItem[" +
                "e=" + e + ", " +
                "origin=" + origin + ']';
    }

}
