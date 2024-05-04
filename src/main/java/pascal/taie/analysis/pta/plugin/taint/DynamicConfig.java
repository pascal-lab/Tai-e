package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.*;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

class DynamicConfig {
    private static final Logger logger = LogManager.getLogger(DynamicConfig.class);

    private final Plugin plugin;

    private final Solver solver;

    private final CSManager csManager;

    DynamicConfig(Plugin plugin, Solver solver) {
        csManager = solver.getCSManager();
        this.plugin = plugin;
        this.solver = solver;
    }

    private void reset() {
        csManager.pointers().forEach(p -> {
            if (p.getPointsToSet() != null) {
                PointsToSet pointsToSet = solver.makePointsToSet();
                for (var v : p.getPointsToSet()) {
                    if (!(v.getObject() instanceof MockObj mockObj &&
                            mockObj.getDescriptor().string().equals("TaintObj"))) {
                        pointsToSet.addObject(v);
                    }
                }
                p.setPointsToSet(pointsToSet);
            }
        });

        // feature: objManager was not reset
        csManager.pointers().forEach(p -> {
            ArrayList<PointerFlowEdge> outEdges = p.getOutEdges().stream()
                    .filter(e -> !(e.kind() == FlowKind.OTHER && e.getInfo().equals("OTHER.TaintTransferEdge")))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            try {
                Field outEdgesF = p.getClass().getSuperclass().getDeclaredField("outEdges");
                outEdgesF.setAccessible(true);
                outEdgesF.set(p, outEdges);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void prepare(String config) {
        try {
            // update the taint config:
            Method updateM = solver.getOptions().getClass().getDeclaredMethod("update", AnalysisOptions.class);
            updateM.setAccessible(true);
            updateM.invoke(solver.getOptions(),  new AnalysisOptions(Map.of( "taint-config", config)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        plugin.setSolver(solver);
        solver.setPlugin(plugin);

        // add entries to WL (for taint analysis)
        // SourceHandler/TransferHandler/... used
        solver.getCallGraph().reachableMethods().forEach(m ->
            m.getEdges().forEach(plugin::onNewCallEdge));
        solver.getCallGraph().reachableMethods().forEach(plugin::onNewCSMethod);

        for (var m: solver.getCallGraph().reachableMethods().toList()) {
            m.getMethod().getIR().forEach(stmt -> plugin.onNewStmt(stmt, m.getMethod()));
            // para taints handled here:
            m.getMethod().getIR().getParams()
                    .forEach(para -> {
                        CSVar csPara = csManager.getCSVar(m.getContext(), para);
                        plugin.onNewPointsToSet(csPara, csPara.getPointsToSet());
                    });
        }
    }

    private void analyze() {
        ((DefaultSolver)solver).analyze();
    }

    void start() {

        String config1 = "src/test/resources/pta/taint/taint-config-2.yml";
        String config2 = "src/test/resources/pta/taint/taint-config.yml";

        reset();
        prepare(config1);
        logger.info("Start analysis with config: {}", config1);
        analyze();


        reset();
        prepare(config2);
        logger.info("Start analysis with config: {}", config2);
        analyze();

//        reset();
//        prepare(config1);
//        logger.info("Start analysis with config: {}", config1);
//        analyze();
//
//        reset();
//        prepare(config2);
//        logger.info("Start analysis with config: {}", config2);
//        analyze();
    }
}
