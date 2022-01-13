/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.config.ConfigException;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.List;

public class CallGraphBuilder extends ProgramAnalysis {

    public static final String ID = "cg";

    private static final Logger logger = LogManager.getLogger(CallGraphBuilder.class);

    private final String algorithm;

    public CallGraphBuilder(AnalysisConfig config) {
        super(config);
        algorithm = config.getOptions().getString("algorithm");
    }

    @Override
    public CallGraph<Invoke, JMethod> analyze() {
        CGBuilder<Invoke, JMethod> builder = switch (algorithm) {
            case "pta", "cipta", "cspta" -> new PTABasedBuilder(algorithm);
            case "cha" -> new CHABuilder();
            default -> throw new ConfigException(
                    "Unknown call graph building algorithm: " + algorithm);
        };
        CallGraph<Invoke, JMethod> callGraph = builder.build();
        logStatistics(callGraph);
        processOptions(callGraph, getOptions());
        return callGraph;
    }

    private static void logStatistics(CallGraph<Invoke, JMethod> callGraph) {
        logger.info("Call graph has {} reachable methods and {} edges",
                callGraph.getNumberOfMethods(),
                callGraph.getNumberOfEdges());
    }

    private static void processOptions(CallGraph<Invoke, JMethod> callGraph,
                                       AnalysisOptions options) {
        String action = options.getString("action");
        if (action == null) {
            return;
        }
        switch (action) {
            case "dump" -> {
                String file = options.getString("file");
                CallGraphs.dumpCallGraph(callGraph, file);
            }
            case "dump-recall" -> {
                List<String> files = (List<String>) options.get("file");
                CallGraphs.dumpMethods(callGraph, files.get(0));
                CallGraphs.dumpCallEdges(callGraph, files.get(1));
            }
            case "compare" -> {
                String file = options.getString("file");
                CallGraphs.compareCallGraph(callGraph, file);
            }
        }
    }
}
