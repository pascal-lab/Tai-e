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
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

/**
 * Taint Analysis composites plugins {@link SourceHandler}, {@link TransferHandler}
 * and {@link SanitizerHandler} to handle the logic associated with {@link Source},
 * {@link TaintTransfer}, and {@link Sanitizer} respectively.
 * The analysis finally gathers taint flows from {@link Sink} through {@link SinkHandler}
 * and generates reports.
 * <br>
 * The following diagram illustrates the workflow of the taint analysis:
 * <pre><code class='text'>
 *     ┌───────────────────────────┐   ┌─────────────────────────────┐
 * ┌──►│       initialize()        ├───┤Clean Up                     │
 * │   └─────────────┬─────────────┘   │                             │
 * │      on-the-fly │with PTA         │ 1.Clear composited handlers │
 * │                 ▼                 │ 2.Remove taint objects      │
 * │   ┌───────────────────────────┐   │   from points-to set        │
 * │   │      onPhaseFinish()      │   │ 3.Remove taint transfer     │
 * │Yes│ ┌───────────────────────┐ │   │   edge from pointer flow    │
 * │ ┌─┼─┤In interactive mode and│ │   │   graph                     │
 * └─┼─┼─┤Enter 'r' from console?│ │   │                             │
 *   │ │ └───────────┬───────────┘ │   │Start Up                     │
 *   │ │             │             │   │                             │
 *   │ └─────────────┼─────────────┘   │ 4. Load taint configuration │
 *   │               │No               │ 5. Create Source/Transfer/  │
 *   │               ▼                 │    Sanitizer handlers       │
 *   │ ┌───────────────────────────┐   │ 6. Create taint objects     │
 *   │ │         onFinish()        │   └─────────────────────────────┘
 *   │ └─────────────┬─────────────┘
 *   │               │
 *   │               ▼                 ┌─────────────────────────────┐
 *   │ ┌───────────────────────────┐   │Collect taint analysis result│
 *   └►│     reportTaintFlows()    ├───┤and report taint flows       │
 *     └───────────────────────────┘   └─────────────────────────────┘
 * </code></pre>
 */
public class TaintAnalysis extends CompositePlugin {

    private static final Logger logger = LogManager.getLogger(TaintAnalysis.class);

    private static final String TAINT_FLOW_GRAPH_FILE = "taint-flow-graph.dot";

    private Solver solver;

    private boolean isInteractive;

    /**
     * Indicates whether the taint analysis result has been reported.
     * It is used to ensures that {@link #reportTaintFlows()} executes only once
     * during a taint analysis.
     */
    private boolean isReported;

    private HandlerContext context;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        isInteractive = solver.getOptions().getBoolean("taint-interactive-mode");
        initialize();
    }

    private void initialize() {
        // clean composited plugins, taint objects and taint transfer edges
        isReported = false;
        clearPlugins();
        if (context != null) {
            TaintManager manager = context.manager();
            solver.getCSManager().pointers().forEach(p -> {
                PointsToSet pts = p.getPointsToSet();
                if (pts != null) {
                    pts.removeIf(csObj -> manager.isTaint(csObj.getObject()));
                }
                p.removeEdgesIf(TaintTransferEdge.class::isInstance);
            });
        }
        // load taint configuration and create new handlers
        TaintConfig config = TaintConfig.loadConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        logger.info(config);
        context = new HandlerContext(solver, new TaintManager(
                solver.getHeapModel()), config);
        addPlugin(new SourceHandler(context),
                new TransferHandler(context),
                new SanitizerHandler(context));
        // trigger the creation of taint objects
        CallGraph<CSCallSite, CSMethod> cg = solver.getCallGraph();
        if (cg != null) {
            CSManager csManager = solver.getCSManager();
            boolean handleStmt = context.config().callSiteMode()
                    || context.config().sources().stream().anyMatch(FieldSource.class::isInstance);
            cg.reachableMethods().forEach(csMethod -> {
                JMethod method = csMethod.getMethod();
                Context ctxt = csMethod.getContext();
                IR ir = csMethod.getMethod().getIR();
                if (handleStmt) {
                    ir.forEach(stmt -> onNewStmt(stmt, method));
                }
                this.onNewCSMethod(csMethod);
                csMethod.getEdges().forEach(this::onNewCallEdge);
                ir.getParams().forEach(param -> {
                    CSVar csParam = csManager.getCSVar(ctxt, param);
                    onNewPointsToSet(csParam, csParam.getPointsToSet());
                });
            });
        }
    }

    @Override
    public void onPhaseFinish() {
        if (isInteractive) {
            while (true) {
                reportTaintFlows();
                System.out.println("Taint Analysis is in interactive mode,"
                        + " you can modify the taint configuration and run the analysis again.\n"
                        + "Enter 'r' to run, 'e' to exit: ");
                String input = readLineFromConsole();
                if (input == null) {
                    break;
                }
                input = input.strip();
                System.out.println("You have entered: '" + input + "'");
                if ("r".equals(input)) {
                    initialize();
                    if (!context.manager().getTaintObjs().isEmpty()) {
                        break;
                    }
                } else if ("e".equals(input)) {
                    isInteractive = false;
                    break;
                }
            }
        }
    }

    /**
     * A utility method for reading one line from the console using {@code System.in}.
     * This method does not use buffering to ensure it does not read more than necessary.
     * <br>
     *
     * @return one line line read from the console,
     * or {@code null} if no line is available
     */
    @Nullable
    private static String readLineFromConsole() {
        StringBuilder sb = new StringBuilder();
        try {
            int c;
            while ((c = System.in.read()) != -1) {
                if (c == '\r' || c == '\n') {
                    return sb.toString();
                }
                sb.append((char) c);
            }
        } catch (Exception e) {
            logger.error("Error reading from console", e);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    @Override
    public void onFinish() {
        reportTaintFlows();
    }

    private void reportTaintFlows() {
        if (isReported) {
            return;
        }
        isReported = true;
        Set<TaintFlow> taintFlows = new SinkHandler(context).collectTaintFlows();
        logger.info("Detected {} taint flow(s):", taintFlows.size());
        taintFlows.forEach(logger::info);
        solver.getResult().storeResult(getClass().getName(), taintFlows);
        TaintManager manager = context.manager();
        Timer.runAndCount(() -> new TFGDumper().dump(
                        new TFGBuilder(solver.getResult(), taintFlows, manager).build(),
                        new File(World.get().getOptions().getOutputDir(), TAINT_FLOW_GRAPH_FILE)),
                "TFGDumper");
    }
}
