package pascal.taie.analysis.bugfinder.nullpointer;

import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;

public class NPEVarVisitor implements StmtVisitor<Var> {
    @Override
    public Var visit(LoadField stmt) {
        return stmt.isStatic() ?
                null : ((InstanceFieldAccess) stmt.getFieldAccess()).getBase();
    }

    @Override
    public Var visit(StoreField stmt) {
        return stmt.isStatic() ?
                null : ((InstanceFieldAccess) stmt.getFieldAccess()).getBase();
    }

    @Override
    public Var visit(Unary stmt) {
        return stmt.getRValue() instanceof ArrayLengthExp ?
                ((ArrayLengthExp) stmt.getRValue()).getBase() : null;
    }

    @Override
    public Var visit(Invoke stmt) {
        return stmt.isStatic() || stmt.isDynamic() ?
                null : ((InvokeInstanceExp) stmt.getInvokeExp()).getBase();
    }

    @Override
    public Var visit(Throw stmt) {
        return stmt.getExceptionRef();
    }

    @Override
    public Var visit(Monitor stmt) {
        return StmtVisitor.super.visit(stmt);
    }

    @Override
    public Var visit(LoadArray stmt) {
        return stmt.getArrayAccess().getBase();
    }

    @Override
    public Var visit(StoreArray stmt) {
        return stmt.getArrayAccess().getBase();
    }
}
