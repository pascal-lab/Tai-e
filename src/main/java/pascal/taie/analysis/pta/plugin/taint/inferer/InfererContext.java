package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintManager;

public record InfererContext(Solver solver,
                             TaintManager taintManager,
                             TaintConfig config,
                             TransferGenerator generator) {
}
