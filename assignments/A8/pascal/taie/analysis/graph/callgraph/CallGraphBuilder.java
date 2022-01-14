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
import pascal.taie.config.ConfigException;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Comparator;

public class CallGraphBuilder extends ProgramAnalysis {

    public static final String ID = "cg";

    private static final Logger logger = LogManager.getLogger(CallGraphBuilder.class);

    /**
     * Separator between call site and its callees.
     */
    private static final String SEP = " -> ";

    private final String algorithm;

    public CallGraphBuilder(AnalysisConfig config) {
        super(config);
        algorithm = config.getOptions().getString("algorithm");
    }

    @Override
    public CallGraph<Invoke, JMethod> analyze() {
        CGBuilder<Invoke, JMethod> builder = switch (algorithm) {
            case "pta", "cipta", "cspta" -> new PTABasedBuilder(algorithm);
            default -> throw new ConfigException(
                    "Unknown call graph building algorithm: " + algorithm);
        };
        CallGraph<Invoke, JMethod> callGraph = builder.build();
        takeAction(callGraph);
        return callGraph;
    }

    private void takeAction(CallGraph<Invoke, JMethod> callGraph) {
        String action = getOptions().getString("action");
        if (action == null) {
            return;
        }
        if (action.equals("dump")) {
            logCallGraph(callGraph);
            String file = getOptions().getString("file");
            CallGraphs.dumpCallGraph(callGraph, file);
        }
    }

    static void logCallGraph(CallGraph<Invoke, JMethod> callGraph) {
        Comparator<JMethod> cmp = Comparator.comparing(JMethod::toString);
        logger.info("#reachable methods: {}", callGraph.getNumberOfMethods());
        logger.info("---------- Reachable methods: ----------");
        callGraph.reachableMethods()
                .sorted(cmp)
                .forEach(logger::info);
        logger.info("\n#call graph edges: {}", callGraph.getNumberOfEdges());
        logger.info("---------- Call graph edges: ----------");
        callGraph.reachableMethods()
                .sorted(cmp) // sort reachable methods
                .forEach(caller ->
                        callGraph.callSitesIn(caller)
                                .sorted(Comparator.comparing(Invoke::getIndex))
                                .filter(callSite -> callGraph.getCalleesOf(callSite).isEmpty())
                                .forEach(callSite ->
                                        logger.info(toString(callSite) + SEP +
                                                toString(callGraph.getCalleesOf(callSite)))));
        logger.info("----------------------------------------");
    }

    private static String toString(Invoke invoke) {
        return invoke.getContainer() + IRPrinter.toString(invoke);
    }

    private static String toString(Collection<JMethod> methods) {
        return methods.stream()
                .sorted(Comparator.comparing(JMethod::toString))
                .toList()
                .toString();
    }
}
