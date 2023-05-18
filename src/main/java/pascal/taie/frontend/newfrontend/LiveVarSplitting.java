package pascal.taie.frontend.newfrontend;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

class LiveVarSplitting extends AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

    protected LiveVarSplitting(CFG<Stmt> cfg) {
        super(cfg);
    }

    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public SetFact<Var> newBoundaryFact() {
        return newInitialFact();
    }

    @Override
    public SetFact<Var> newInitialFact() {
        return new SetFact<>();
    }

    @Override
    public void meetInto(SetFact<Var> vars, SetFact<Var> target) {
        target.union(vars);
    }

    @Override
    public boolean transferNode(Stmt stmt, SetFact<Var> in, SetFact<Var> out) {
        SetFact<Var> oldIn = in.copy();
        in.set(out);

        stmt.getDef().ifPresent(def -> {
            if (def instanceof Var) {
                in.remove((Var) def);
            }
        });

        for (RValue use : stmt.getUses()) {
            if (use instanceof Var var) {
                in.add(var);
            }
        }

        return ! in.equals(oldIn);
    }
}
