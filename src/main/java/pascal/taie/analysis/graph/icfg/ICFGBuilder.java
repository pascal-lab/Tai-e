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

package pascal.taie.analysis.graph.icfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.CFGDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Configs;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.ObjectIdMapper;
import pascal.taie.util.SimpleMapper;
import pascal.taie.util.graph.DotDumper;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public class ICFGBuilder extends ProgramAnalysis {

    public static final String ID = "icfg";

    private static final Logger logger = LogManager.getLogger(ICFGBuilder.class);

    private final boolean isDump;

    public ICFGBuilder(AnalysisConfig config) {
        super(config);
        isDump = getOptions().getBoolean("dump");
    }

    @Override
    public ICFG<JMethod, Stmt> analyze() {
        CallGraph<Stmt, JMethod> callGraph = World.get().getResult(CallGraphBuilder.ID);
        ICFG<JMethod, Stmt> icfg = new DefaultICFG(callGraph);
        if (isDump) {
            dumpICFG(icfg);
        }
        return icfg;
    }

    private static void dumpICFG(ICFG<JMethod, Stmt> icfg) {
        String dotPath = new File(Configs.getOutputDir(),
                icfg.entryMethods()
                        .map(m -> m.getDeclaringClass() + "." + m.getName())
                        .collect(Collectors.joining("-")) + "-icfg.dot")
                .toString();
        logger.info("Dumping ICFG to {} ...", dotPath);
        ObjectIdMapper<Stmt> mapper = new SimpleMapper<>();
        new DotDumper<Stmt>()
                .setNodeToString(n -> Integer.toString(mapper.getId(n)))
                .setNodeLabeler(n -> toLabel(n, icfg))
                .setGlobalNodeAttributes(Map.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeAttrs(e -> {
                    if (e instanceof CallEdge) {
                        return Map.of("style", "dashed", "color", "blue");
                    } else if (e instanceof ReturnEdge) {
                        return Map.of("style", "dashed", "color", "red");
                    } else if (e instanceof CallToReturnEdge) {
                        return Map.of("style", "dashed");
                    } else {
                        return Map.of();
                    }
                })
                .dump(icfg, dotPath);
    }

    private static String toLabel(Stmt stmt, ICFG<JMethod, Stmt> icfg) {
        JMethod method = icfg.getContainingMethodOf(stmt);
        CFG<Stmt> cfg = getCFGOf(method);
        return CFGDumper.toLabel(stmt, cfg);
    }

    static CFG<Stmt> getCFGOf(JMethod method) {
        return method.getIR().getResult(CFGBuilder.ID);
    }
}
