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
 * Collects statements in program that the client wants.
 */
abstract class Collector extends ProgramAnalysis<StmtResult<Boolean>> {

    private static final Logger logger = LogManager.getLogger(Collector.class);

    Collector(AnalysisConfig config) {
        super(config);
    }

    @Override
    public StmtResult<Boolean> analyze() {
        PointerAnalysisResult result = World.get().getResult(PointerAnalysis.ID);
        CallGraph<Invoke, JMethod> callGraph = result.getCallGraph();
        Set<Stmt> wantedStmts = Sets.newSet();
        int nRelevantStmts = 0;
        int nWantedAppStmts = 0, nRelevantAppStmts = 0;
        // collect want statements and count
        for (JMethod method : callGraph) {
            boolean isApp = method.isApplication();
            for (Stmt stmt : method.getIR()) {
                if (isRelevant(stmt)) {
                    ++nRelevantStmts;
                    if (isApp) {
                        ++nRelevantAppStmts;
                    }
                    if (isWanted(stmt, result)) {
                        wantedStmts.add(stmt);
                        if (isApp) {
                            ++nWantedAppStmts;
                        }
                    }
                }
            }
        }
        // log statistics
        logger.info("#{}: found {} in {} reachable relevant Stmts",
                getDescription(), wantedStmts.size(), nRelevantStmts);
        logger.info("#{}: found {} in {} reachable relevant Stmts (app)",
                getDescription(), nWantedAppStmts, nRelevantAppStmts);
        // convert result to StmtResult
        return new StmtResult<>() {

            @Override
            public boolean isRelevant(Stmt stmt) {
                return Collector.this.isRelevant(stmt);
            }

            @Override
            public Boolean getResult(Stmt stmt) {
                return wantedStmts.contains(stmt);
            }
        };
    }

    /**
     * @return {@code true} if the given statement is relevant to the client.
     */
    abstract boolean isRelevant(Stmt stmt);

    /**
     * @return {@code true} if the given statement is wanted by the client.
     */
    abstract boolean isWanted(Stmt stmt, PointerAnalysisResult result);

    /**
     * @return description of wanted statements
     */
    abstract String getDescription();
}
