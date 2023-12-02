package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;

import java.util.List;

public interface IVarManager {

    /**
     * Generate a new temporary variable
     * @return the new temporary variable
     */
    Var getTempVar();

    Var splitVar(Var var, int index);

    /**
     * @return parameters except `this`.
     */
    List<Var> getParams();

    Var[] getLocals();

    default Var[] getNonSSAVar() {
        return getLocals();
    }
}
