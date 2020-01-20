package sa.dataflow.cp;

import sa.dataflow.analysis.DataFlowAnalysis;
import sa.dataflow.solver.Solver;
import sa.dataflow.solver.SolverFactory;
import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Map;

public class ConstantPropagation extends BodyTransformer {

    private DataFlowAnalysis<FlowMap, Unit> problem = new Analysis();

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Solver<FlowMap, Unit> solver = SolverFactory.v().newSolver(problem, cfg);
        solver.solve();
        outputResult(b, solver.getAnalysisResult());
    }

    private void outputResult(Body body, Map<Unit, FlowMap> result) {
        System.out.println("------ " + body.getMethod() + " -----");
        result.forEach((k, v) -> System.out.println(k + "\n" + v));
    }
}
