package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

public class PhiStmt extends DefinitionStmt<Var, PhiExp> {

    public PhiStmt(Var v) {

    }

    @Nullable
    @Override
    public Var getLValue() {
        return null;
    }

    @Override
    public PhiExp getRValue() {
        return null;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return null;
    }
}
