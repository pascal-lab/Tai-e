/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import java.io.File;
import java.util.Set;

public class TaintAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(TaintAnalysis.class);

    private static final String TAINT_FLOW_GRAPH_FILE = "taint-flow-graph.dot";

    private Solver solver;

    private TaintManager manager;

    private Plugin onFlyHandler;

    private SinkHandler sinkHandler;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        manager = new TaintManager(solver.getHeapModel());
        TaintConfig config = TaintConfig.loadConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        logger.info(config);
        HandlerContext context = new HandlerContext(solver, manager, config);
        CompositePlugin onFlyHandler = new CompositePlugin();
        onFlyHandler.addPlugin(
                new SourceHandler(context),
                new TransferHandler(context),
                new SanitizerHandler(context));
        this.onFlyHandler = onFlyHandler;
        sinkHandler = new SinkHandler(context);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        onFlyHandler.onNewCallEdge(edge);
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        onFlyHandler.onNewStmt(stmt, container);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        onFlyHandler.onNewCSMethod(csMethod);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        onFlyHandler.onNewPointsToSet(csVar, pts);
    }

    @Override
    public void onFinish() {
        Set<TaintFlow> taintFlows = sinkHandler.collectTaintFlows();
        solver.getResult().storeResult(getClass().getName(), taintFlows);
        logger.info("Detected {} taint flow(s):", taintFlows.size());
        taintFlows.forEach(logger::info);
        Timer.runAndCount(() -> new TFGDumper().dump(
                        new TFGBuilder(solver.getResult(), taintFlows, manager).build(),
                        new File(World.get().getOptions().getOutputDir(), TAINT_FLOW_GRAPH_FILE)),
                "TFGDumper");
    }
}
