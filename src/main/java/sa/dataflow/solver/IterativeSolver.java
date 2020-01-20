package sa.dataflow.solver;

import sa.dataflow.analysis.DataFlowAnalysis;
import soot.toolkits.graph.DirectedGraph;

class IterativeSolver<Domain, Node> extends Solver<Domain, Node> {

    IterativeSolver(DataFlowAnalysis<Domain, Node> problem,
                    DirectedGraph<Node> cfg) {
        super(problem, cfg);
    }

    @Override
    protected void solveFixedPoint(DirectedGraph<Node> cfg) {
        boolean changed;
        do {
            changed = false;
            for (Node node : cfg) {
                if (!cfg.getHeads().contains(node)) {
                    Domain in = cfg.getPredsOf(node)
                            .stream()
                            .map(outFlow::get)
                            .reduce(problem.newInitialValue(), problem::meet);
                    Domain out = outFlow.get(node);
                    changed |= problem.transfer(in, node, out);
                }
            }
        } while (changed);
    }
}
