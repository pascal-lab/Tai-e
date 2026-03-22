package pascal.taie.analysis.pta.plugin.cutshortcut.field;

import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.FieldStmt;
import pascal.taie.ir.stmt.StmtVisitor;

public class AbstractLoadField extends FieldStmt<Var, FieldAccess> {
    private final boolean terminate;

    public AbstractLoadField(Var lvalue, FieldAccess rvalue, boolean terminate) {
        super(lvalue, rvalue);
        this.terminate = terminate;
    }

    @Override
    public FieldAccess getFieldAccess() {
        return getRValue();
    }

    public boolean isNonRelay() {
        return !terminate;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return null;
    }
}
