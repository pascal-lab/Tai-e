package sa.dataflow.livevar;

import sa.dataflow.analysis.DataFlowAnalysis;
import sa.dataflow.lattice.DataFlowTag;
import sa.dataflow.lattice.FlowSet;
import sa.dataflow.lattice.FlowSetFactory;
import sa.dataflow.solver.Solver;
import sa.dataflow.solver.SolverFactory;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Comparator;
import java.util.Map;

public class LiveVariableAnalysis extends BodyTransformer
        implements DataFlowAnalysis<FlowSet<Local>, Unit> {

    private static final LiveVariableAnalysis INSTANCE = new LiveVariableAnalysis();

    public static LiveVariableAnalysis v() {
        return INSTANCE;
    }

    private FlowSetFactory<Local> flowSetFactory;

    private LiveVariableAnalysis() {
        flowSetFactory = FlowSetFactory.getFactory();
    }

    // ---------- Data-flow analysis for live variable analysis  ----------
    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public FlowSet<Local> getEntryInitialValue() {
        return newInitialValue();
    }

    @Override
    public FlowSet<Local> newInitialValue() {
        return flowSetFactory.newFlowSet();
    }

    @Override
    public FlowSet<Local> meet(FlowSet<Local> v1, FlowSet<Local> v2) {
        return v1.duplicate().union(v2);
    }

    @Override
    public boolean transfer(FlowSet<Local> in, Unit unit, FlowSet<Local> out) {
        FlowSet<Local> oldOut = out.duplicate();
        out.setTo(in);
        // Kill definitions in unit
        unit.getDefBoxes()
                .stream()
                .map(ValueBox::getValue)
                .filter(v -> v instanceof Local)
                .map(v -> (Local) v)
                .forEach(out::remove);
        // Generate uses in unit
        unit.getUseBoxes()
                .stream()
                .map(ValueBox::getValue)
                .filter(v -> v instanceof Local)
                .map(v -> (Local) v)
                .forEach(out::add);
        return !out.equals(oldOut);
    }

    // ---------- Body transformer ----------
    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Solver<FlowSet<Local>, Unit> solver = SolverFactory.v().newSolver(this, cfg);
        solver.solve();
        b.addTag(new DataFlowTag<>("LiveVarTag", solver.getAfterFlow()));
        outputResult(b, solver.getAfterFlow());
    }

    private synchronized void outputResult(Body body, Map<Unit, FlowSet<Local>> result) {
        System.out.println("------ " + body.getMethod() + " -----");
        body.getUnits().forEach(u ->
                System.out.println("L" + u.getJavaSourceStartLineNumber()
                        + "{" + u + "}"
                        + ": " + result.get(u)));
    }
}
