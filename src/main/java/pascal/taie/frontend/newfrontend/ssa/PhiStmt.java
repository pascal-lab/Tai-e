package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.StmtVisitor;

import javax.annotation.Nullable;

public class PhiStmt extends DefinitionStmt<Var, PhiExp> {

    private final Var base;

    private final Var def; // renamed var, different from base

    private final PhiExp phiExp;

    private boolean dead;

    public PhiStmt(Var base, Var def, PhiExp phiExp) {
        this.base = base;
        this.def = def;
        this.phiExp = phiExp;
    }

    public Var getBase() {
        return base;
    }

    public void markDead() {
        this.dead = true;
    }

    @Nullable
    @Override
    public Var getLValue() {
        return def;
    }

    @Override
    public PhiExp getRValue() {
        return phiExp;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return def + " = " + phiExp + (dead ? " (dead)" : "");
    }
}
