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
import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigUtils;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.graph.GraphDumper;

import java.io.File;

public class ICFGBuilder extends InterproceduralAnalysis {

    private static final Logger logger = LogManager.getLogger(ICFGBuilder.class);

    public static final String ID = "icfg";

    private final boolean isDump;

    public ICFGBuilder(AnalysisConfig config) {
        super(config);
        isDump = getOptions().getBoolean("dump");
    }

    @Override
    public ICFG<JMethod, Stmt> analyze() {
        CallGraph<Stmt, JMethod> callGraph = World.getResult(CallGraphBuilder.ID);
        ICFG<JMethod, Stmt> icfg = new DefaultICFG(callGraph);
        if (isDump) {
            File outFile = new File(ConfigUtils.getOutputDir(), "icfg.dot");
            logger.info("Dumping call graph to {} ...", outFile);
            GraphDumper.dumpDotFile(icfg, outFile.getPath(),
                    stmt -> toString(stmt, icfg));
        }
        return icfg;
    }

    private static String toString(Stmt stmt, ICFG<JMethod, Stmt> icfg) {
        JMethod method = icfg.getContainingMethodOf(stmt);
        CFG<Stmt> cfg = getCFGOf(method);
        if (cfg.isEntry(stmt)) {
            return "ENTRY" + method;
        } else if (cfg.isExit(stmt)) {
            return "EXIT" + method;
        } else {
            return String.format("%d@L%d:%s",
                    stmt.getIndex(), stmt.getLineNumber(), stmt);
        }
    }

    static CFG<Stmt> getCFGOf(JMethod method) {
        return method.getIR().getResult(CFGBuilder.ID);
    }
}
