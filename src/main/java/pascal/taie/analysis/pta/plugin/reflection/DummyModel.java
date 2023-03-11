package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

/**
 * Dummy inference model that does nothing.
 */
class DummyModel extends InferenceModel {

    DummyModel(Solver solver) {
        super(solver, null, null);
    }

    @Override
    protected void handleNewNonInvokeStmt(Stmt stmt) {
    }

    @Override
    protected void classForName(CSVar csVar, PointsToSet pts, Invoke invoke) {
    }

    @Override
    protected void getConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
    }

    @Override
    protected void getDeclaredConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
    }

    @Override
    protected void getMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
    }

    @Override
    protected void getDeclaredMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
    }

    @Override
    protected void registerVarAndHandler() {
    }
}
