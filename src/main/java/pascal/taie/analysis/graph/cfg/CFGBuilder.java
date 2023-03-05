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

package pascal.taie.analysis.graph.cfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.exception.CatchAnalysis;
import pascal.taie.analysis.exception.CatchResult;
import pascal.taie.analysis.exception.ThrowAnalysis;
import pascal.taie.analysis.exception.ThrowResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.type.ClassType;

import java.io.File;
import java.util.Set;
import java.util.function.Predicate;

public class CFGBuilder extends MethodAnalysis<CFG<Stmt>> {

    public static final String ID = "cfg";

    private static final Logger logger = LogManager.getLogger(CFGBuilder.class);

    private static final String CFG_DIR = "cfg";

    private final boolean noException;

    private final boolean isDump;

    private final File dumpDir;

    public CFGBuilder(AnalysisConfig config) {
        super(config);
        noException = getOptions().getString("exception") == null;
        isDump = getOptions().getBoolean("dump");
        if (isDump) {
            dumpDir = new File(World.get().getOptions().getOutputDir(), CFG_DIR);
            if (!dumpDir.exists()) {
                dumpDir.mkdirs();
            }
            logger.info("Dumping CFGs in {}", dumpDir.getAbsolutePath());
        } else {
            dumpDir = null;
        }
    }

    @Override
    public CFG<Stmt> analyze(IR ir) {
        StmtCFG cfg = new StmtCFG(ir);
        cfg.setEntry(new Nop());
        cfg.setExit(new Nop());
        buildNormalEdges(cfg);
        if (!noException) {
            buildExceptionalEdges(cfg);
        }
        if (isDump) {
            CFGDumper.dumpDotFile(cfg, dumpDir);
        }
        return cfg;
    }

    private static void buildNormalEdges(StmtCFG cfg) {
        IR ir = cfg.getIR();
        cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.ENTRY, cfg.getEntry(), ir.getStmt(0)));
        for (int i = 0; i < ir.getStmts().size(); ++i) {
            Stmt curr = ir.getStmt(i);
            cfg.addNode(curr);
            if (curr instanceof Goto) {
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.GOTO,
                        curr, ((Goto) curr).getTarget()));
            } else if (curr instanceof If) {
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.IF_TRUE,
                        curr, ((If) curr).getTarget()));
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.IF_FALSE,
                        curr, ir.getStmt(i + 1)));
            } else if (curr instanceof SwitchStmt switchStmt) {
                switchStmt.getCaseTargets().forEach(pair -> {
                    int caseValue = pair.first();
                    Stmt target = pair.second();
                    cfg.addEdge(new SwitchCaseEdge<>(
                            switchStmt, target, caseValue));
                });
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.SWITCH_DEFAULT,
                        switchStmt, switchStmt.getDefaultTarget()));
            } else if (curr instanceof Return) {
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.RETURN, curr, cfg.getExit()));
            } else if (curr.canFallThrough() &&
                    i + 1 < ir.getStmts().size()) { // Defensive check
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.FALL_THROUGH,
                        curr, ir.getStmt(i + 1)));
            }
        }
    }

    /**
     * Builds exceptional edges for given {@code cfg}.
     * <p>
     * Note that if a statement throws an exception, it means that the
     * execution of the statement does not complete, so that the exceptional
     * control-flow should not pass through it. Hence, we build exceptional
     * edges from the predecessors of the throwable statements to the
     * exceptional control-flow targets (either relevant catch statements
     * or the method exit).
     * <p>
     * For example, in this code snippet:
     * <pre>
     * 1 try {
     * 2     x = 0;
     * 3     o.foo();
     * 4 } catch (NullPointerException e) { ... }
     * </pre>
     * We build an exceptional edge (with NPE) from line 2 to line 4,
     * since if {@code o.foo();} at line 3 throws a NPE, the method
     * invocation is not executed at all.
     *
     * @param cfg the basic control-flow graph which the exceptional edges
     *            are added to.
     */
    private static void buildExceptionalEdges(StmtCFG cfg) {
        IR ir = cfg.getIR();
        ThrowResult throwResult = ir.getResult(ThrowAnalysis.ID);
        CatchResult catchResult = CatchAnalysis.analyze(ir, throwResult);
        ir.forEach(stmt -> {
            // build edges for implicit exceptions
            catchResult.getCaughtImplicitOf(stmt).forEachSet((catcher, exceptions) ->
                    cfg.getInEdgesOf(stmt)
                            .stream()
                            .filter(Predicate.not(CFGEdge::isExceptional))
                            .map(CFGEdge::source)
                            .forEach(pred ->
                                    cfg.addEdge(new ExceptionalEdge<>(
                                            CFGEdge.Kind.CAUGHT_EXCEPTION,
                                            pred, catcher, exceptions))));
            Set<ClassType> uncaught = catchResult.getUncaughtImplicitOf(stmt);
            if (!uncaught.isEmpty()) {
                cfg.getInEdgesOf(stmt)
                        .stream()
                        .filter(Predicate.not(CFGEdge::isExceptional))
                        .map(CFGEdge::source)
                        .forEach(pred -> cfg.addEdge(
                                new ExceptionalEdge<>(
                                        CFGEdge.Kind.UNCAUGHT_EXCEPTION,
                                        pred, cfg.getExit(), uncaught)));
            }
            // build edges for explicit exceptions
            if (stmt instanceof Throw || stmt instanceof Invoke) {
                catchResult.getCaughtExplicitOf(stmt).forEachSet((catcher, exceptions) ->
                        cfg.addEdge(new ExceptionalEdge<>(
                                CFGEdge.Kind.CAUGHT_EXCEPTION,
                                stmt, catcher, exceptions))
                );
                Set<ClassType> uncaughtEx = catchResult.getUncaughtExplicitOf(stmt);
                if (!uncaughtEx.isEmpty()) {
                    cfg.addEdge(new ExceptionalEdge<>(
                            CFGEdge.Kind.UNCAUGHT_EXCEPTION,
                            stmt, cfg.getExit(), uncaughtEx));
                }
            }
        });
    }
}
