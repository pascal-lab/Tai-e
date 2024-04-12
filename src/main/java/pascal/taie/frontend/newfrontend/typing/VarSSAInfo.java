package pascal.taie.frontend.newfrontend.typing;

import pascal.taie.ir.exp.Var;

import java.util.BitSet;

public class VarSSAInfo {
    private final BitSet isSSA;

    public VarSSAInfo() {
        isSSA = new BitSet();
    }

    public boolean isSSAVar(Var v) {
        return isSSA.get(v.getIndex());
    }

    public void setSSA(Var v) {
        isSSA.set(v.getIndex(), true);
    }

    public void setNonSSA(Var v) {
        isSSA.set(v.getIndex(), false);
    }
}
