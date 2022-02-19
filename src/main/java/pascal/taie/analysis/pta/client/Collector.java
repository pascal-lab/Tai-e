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

package pascal.taie.analysis.pta.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.StmtResult;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Set;

/**
 * Collects statements in reachable program that fulfills specific conditions.
 */
abstract class Collector extends ProgramAnalysis {

    private static final Logger logger = LogManager.getLogger(Collector.class);

    Collector(AnalysisConfig config) {
        super(config);
    }

    @Override
    public StmtResult<Boolean> analyze() {
        PointerAnalysisResult result = World.get().getResult(PointerAnalysis.ID);
        CallGraph<Invoke, JMethod> callGraph = result.getCallGraph();
        Set<Stmt> wantStmts = Sets.newSet();
        int nRelevantStmts = 0;
        int nWantAppStmts = 0, nRelevantAppStmts = 0;
        // collect polymorphic calls and count
        for (JMethod method : callGraph) {
            boolean isApp = method.getDeclaringClass().isApplication();
            for (Stmt stmt : method.getIR()) {
                if (isRelevant(stmt)) {
                    ++nRelevantStmts;
                    if (isApp) {
                        ++nRelevantAppStmts;
                    }
                    if (want(stmt, result)) {
                        wantStmts.add(stmt);
                        if (isApp) {
                            ++nWantAppStmts;
                        }
                    }
                }
            }
        }
        // log statistics
        logger.info("#{}: found {} in {} reachable relevant Stmts",
                getDescription(), wantStmts.size(), nRelevantStmts);
        logger.info("#{}: found {} in {} reachable relevant Stmts (app)",
                getDescription(), nWantAppStmts, nRelevantAppStmts);
        // convert result to StmtResult
        return new StmtResult<>() {

            @Override
            public boolean isRelevant(Stmt stmt) {
                return Collector.this.isRelevant(stmt);
            }

            @Override
            public Boolean getResult(Stmt stmt) {
                return wantStmts.contains(stmt);
            }
        };
    }

    abstract boolean isRelevant(Stmt stmt);

    abstract boolean want(Stmt stmt, PointerAnalysisResult result);

    abstract String getDescription();
}
