package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.pta.core.solver.Solver;

class DummyModel extends MetaObjModel {

    DummyModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerVarAndHandler() {
    }
}
