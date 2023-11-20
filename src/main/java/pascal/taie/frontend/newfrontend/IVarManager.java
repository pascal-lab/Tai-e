package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;

public interface IVarManager {

    /**
     * Generate a new temporary variable
     * @return the new temporary variable
     */
    Var getTempVar();

    Var splitVar(Var var, int index);

    Var[] getLocals();

    default Var[] getNonSSAVar() {
        return getLocals();
    }
}
