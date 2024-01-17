package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public interface IVarManager {

    /**
     * Generate a new temporary variable
     * @return the new temporary variable
     */
    Var getTempVar();

    @Nullable
    Var getThisVar();

    Var splitVar(Var var, int index);

    /**
     * @return parameters except `this`.
     */
    List<Var> getParams();

    Var[] getLocals();

    default Var[] getNonSSAVar() {
        return getLocals();
    }

    List<Var> getVars();

    void removeAndReindexVars(Predicate<Var> p);
}
