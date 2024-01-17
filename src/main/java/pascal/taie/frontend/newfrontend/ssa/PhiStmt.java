package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.StmtVisitor;

public class PhiStmt extends AssignStmt<Var, PhiExp> {

    private final Var base;

    public PhiStmt(Var base, Var def, PhiExp phiExp) {
        super(def, phiExp);
        this.base = base;
    }

    /**
     * WARNING: this method and the field `base` is used only in the front-end,
     * and you should not use this method to testify may-have-same-base relationship.
     * Instead, check the segment before "#" in the variable name.
     * @return the base variable before SSA renaming.
     */
    public Var getBase() {
        return base;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
