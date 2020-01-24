package sa.dataflow.solver;

import sa.dataflow.analysis.IPDataFlowAnalysis;
import sa.icfg.Edge;
import sa.icfg.ICFG;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

public class IPWorkListSolver<Domain, Method, Node>
        extends IPSolver<Domain, Method, Node> {

    private Queue<Node> workList;

    public IPWorkListSolver(IPDataFlowAnalysis<Domain, Method, Node> analysis,
                            ICFG<Method, Node> icfg) {
        super(analysis, icfg);
        inFlow = new LinkedHashMap<>();
        outFlow = new LinkedHashMap<>();
        edgeFlow = new LinkedHashMap<>();
    }

    @Override
    protected void solveFixedPoint(ICFG<Method, Node> icfg) {
        // TODO - A little too much special cases, to be refactored
        workList = new LinkedList<>(icfg.getHeads());
        while (!workList.isEmpty()) {
            Node node = workList.remove();
            Domain in;
            if (icfg.getInEdgesOf(node).isEmpty()) { // heads of entry methods
                in = inFlow.get(node);
            } else { // other nodes
                in = icfg.getInEdgesOf(node)
                        .stream()
                        .map(edgeFlow::get)
                        .reduce(analysis.newInitialFlow(), analysis::meet);
                inFlow.put(node, in);
            }
            boolean changed = false;
            Domain out = outFlow.get(node);
            if (out == null) { // node has not been visited before
                out = analysis.newInitialFlow();
                outFlow.put(node, out);
                changed = true;
            }
            if (icfg.isCallSite(node)) {
                changed |= analysis.transferCallNode(node, in, out);
            } else {
                changed |= analysis.transfer(node, in, out);
            }
            if (changed) {
                for (Edge<Node> edge : icfg.getOutEdgesOf(node)) {
                    analysis.transferEdge(edge, in, out, edgeFlow.get(edge));
                    workList.add(edge.getTarget());
                }
            }
        }
    }
}
