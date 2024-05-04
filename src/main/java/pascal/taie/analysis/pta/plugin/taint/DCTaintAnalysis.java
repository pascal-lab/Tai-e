package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

public class DCTaintAnalysis extends CompositePlugin {

    private static final Logger logger = LogManager.getLogger(DCTaintAnalysis.class);

    private HandlerContext context;

    private boolean firstFinish = true;

    private TaintAnalysis taintAnalysis;

    @Override
    public void setSolver(Solver solver) {
        TaintManager manager = new TaintManager(solver.getHeapModel());
        TaintConfig config = TaintConfig.loadConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        context = new HandlerContext(solver, manager, config);

        taintAnalysis = new TaintAnalysis();
        taintAnalysis.setSolver(solver);
    }

    @Override
    public void onFinish() {
        taintAnalysis.onFinish();

        boolean dynamicConfigured = true;
//        dynamicConfigured = false;
        if (dynamicConfigured && firstFinish) {
            firstFinish = false;
            logger.info("Dynamic configuration is enabled.");
            try {
                DynamicConfig dynamicConfig = new DynamicConfig(this, context.solver());
                dynamicConfig.start();
            } catch (Exception e) {
                logger.error("Failed to configure dynamic analysis.", e);
            }
        }
    }

    @Override
    public void addPlugin(Plugin... plugins) {
        taintAnalysis.addPlugin(plugins);
    }

    @Override
    public void onStart() {
        taintAnalysis.onStart();
    }

    @Override
    public void onPhaseFinish() {
        taintAnalysis.onPhaseFinish();
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        taintAnalysis.onNewPointsToSet(csVar, pts);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        taintAnalysis.onNewCallEdge(edge);
    }

    @Override
    public void onNewMethod(JMethod method) {
        taintAnalysis.onNewMethod(method);
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        taintAnalysis.onNewStmt(stmt, container);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        taintAnalysis.onNewCSMethod(csMethod);
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        taintAnalysis.onUnresolvedCall(recv, context, invoke);
    }
}
