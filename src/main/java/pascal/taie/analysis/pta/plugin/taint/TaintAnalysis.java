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
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.util.Timer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.Set;

public class TaintAnalysis extends CompositePlugin {

    private static final Logger logger = LogManager.getLogger(TaintAnalysis.class);

    private static final String TAINT_FLOW_GRAPH_FILE = "taint-flow-graph.dot";

    private HandlerContext context;

    private boolean callSiteMode;

    private TaintRestarter restarter;

    @Override
    public void setSolver(Solver solver) {
        TaintManager manager = new TaintManager(solver.getHeapModel());
        TaintConfig config = TaintConfig.loadConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        logger.info(config);
        context = new HandlerContext(solver, manager, config);
        addPlugin(new SourceHandler(context),
                new TransferHandler(context),
                new SanitizerHandler(context));

        callSiteMode = config.callSiteMode();
        restarter = new TaintRestarter(this, context.solver());
    }

    private void sinkAndReport() {
        Set<TaintFlow> taintFlows = new SinkHandler(context).collectTaintFlows();
        logger.info("Detected {} taint flow(s):", taintFlows.size());
        taintFlows.forEach(logger::info);
        Solver solver = context.solver();
        solver.getResult().storeResult(getClass().getName(), taintFlows);
        TaintManager manager = context.manager();
        Timer.runAndCount(() -> new TFGDumper().dump(
                        new TFGBuilder(solver.getResult(), taintFlows, manager).build(),
                        new File(World.get().getOptions().getOutputDir(), TAINT_FLOW_GRAPH_FILE)),
                "TFGDumper");
    }


    public void startFileWatcher(Path filePath) throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        filePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

        WatchKey key;
        while (true) {
            key = watchService.take(); // block until a file event occur

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                } else {
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    if (fileName.toFile().getName().equals(filePath.toFile().getName())) {
                        logger.info("File has been modified: " + filePath);
                        reloadConfigAndRestart();
                        return;
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void reloadConfigAndRestart() {
        logger.info("Reloading configuration and restarting...");
        // ... 读取配置文件 ...
        restarter.start();
    }

    @Override
    public void onPhaseFinish() {
        boolean dynamicConfigured = true;
        if (dynamicConfigured) {
            // report taint flows:
            sinkAndReport();

            // wait for user's input, if `exit`, skip; if `continue`, then try file listen
            System.out.println("Change your taint config and continue.");
            String path = (String) context.solver().getOptions().get("taint-config");
            Path configPath = Paths.get(path);
            try {
                startFileWatcher(configPath);
            } catch (Exception e) {
                e.printStackTrace();
            }

//            Scanner scanner = new Scanner(System.in);
//            while (true) {
//                if (scanner.hasNextLine()) {
//                    String line = scanner.nextLine();
//                    if (line.equals("exit") || line.equals("q")) {
//                        scanner.close();
//                        return;
//                    } else if (line.equals("continue") || line.equals("c")) {
//                        scanner.close();
//                        break;
//                    } else {
//                        System.out.println("Unknown command: " + line);
//                    }
//                }
//            }

            // gen taints
            logger.info("Dynamic configuration is enabled.");
            restarter.start();
        }
    }

    @Override
    public void onFinish() {
        sinkAndReport();
    }

    class TaintRestarter {
        private final TaintAnalysis taintAnalysis;

        private final Solver solver;

        private final CSManager csManager;

        TaintRestarter(TaintAnalysis taintAnalysis, Solver solver) {
            csManager = solver.getCSManager();
            this.taintAnalysis = taintAnalysis;
            this.solver = solver;
        }

        private void reset() {
            csManager.pointers().forEach(p -> {
                    p.rmFromPointsToIf(csObj -> context.manager().isTaint(csObj.getObject()));
                    p.rmFromOutEdgesIf(TaintTransferEdge.class::isInstance);
            });
        }

        private void prepare() {
            // clear its compositing plugins
            taintAnalysis.clearAllPlugins();
            taintAnalysis.setSolver(solver);

            // add entries to WL (for taint analysis)
            // SourceHandler/TransferHandler/... used

            // you have to call this earlier than onNewCSMethod, and it's designed for Call Site Mode
            if (callSiteMode) {
                // Must do it before anything!!!
                solver.getCallGraph().reachableMethods().forEach(
                    m -> m.getMethod().getIR().forEach(stmt -> taintAnalysis.onNewStmt(stmt, m.getMethod()))
                );
            }

            // perhaps use `edges()` ?
            solver.getCallGraph().reachableMethods().forEach(m ->
                    m.getEdges().forEach(taintAnalysis::onNewCallEdge));

            solver.getCallGraph().reachableMethods().forEach(m -> {
                    // field taint
                    taintAnalysis.onNewCSMethod(m);
                    // para taints handled here:
                    m.getMethod().getIR().getParams()
                        .forEach(para -> {
                            CSVar csPara = csManager.getCSVar(m.getContext(), para);
                            taintAnalysis.onNewPointsToSet(csPara, csPara.getPointsToSet());
                        });
            });
        }

        void start() {
            reset();
            prepare();
            logger.info("Start taint analysis!");
        }
    }
}
