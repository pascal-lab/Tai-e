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

package pascal.taie.analysis.graph.cfg;

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

import java.util.Set;
import java.util.function.Predicate;

public class CFGBuilder extends MethodAnalysis {

    public static final String ID = "cfg";

    private final boolean noException;

    private final boolean isDump;

    public CFGBuilder(AnalysisConfig config) {
        super(config);
        noException = getOptions().getString("exception").equals("none");
        isDump = getOptions().getBoolean("dump");
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
            CFGDumper.dumpDotFile(cfg);
        }
        return cfg;
    }

    private static void buildNormalEdges(StmtCFG cfg) {
        IR ir = cfg.getIR();
        cfg.addEdge(new Edge<>(Edge.Kind.ENTRY, cfg.getEntry(), ir.getStmt(0)));
        for (int i = 0; i < ir.getStmts().size(); ++i) {
            Stmt curr = ir.getStmt(i);
            cfg.addNode(curr);
            if (curr instanceof Goto) {
                cfg.addEdge(new Edge<>(Edge.Kind.GOTO,
                        curr, ((Goto) curr).getTarget()));
            } else if (curr instanceof If) {
                cfg.addEdge(new Edge<>(Edge.Kind.IF_TRUE,
                        curr, ((If) curr).getTarget()));
                cfg.addEdge(new Edge<>(Edge.Kind.IF_FALSE,
                        curr, ir.getStmt(i + 1)));
            } else if (curr instanceof SwitchStmt switchStmt) {
                switchStmt.getCaseTargets().forEach(pair -> {
                    int caseValue = pair.first();
                    Stmt target = pair.second();
                    cfg.addEdge(new SwitchCaseEdge<>(
                            switchStmt, target, caseValue));
                });
                cfg.addEdge(new Edge<>(Edge.Kind.SWITCH_DEFAULT,
                        switchStmt, switchStmt.getDefaultTarget()));
            } else if (curr instanceof Return) {
                cfg.addEdge(new Edge<>(Edge.Kind.RETURN, curr, cfg.getExit()));
            } else if (curr.canFallThrough() &&
                    i + 1 < ir.getStmts().size()) { // Defensive check
                cfg.addEdge(new Edge<>(Edge.Kind.FALL_THROUGH,
                        curr, ir.getStmt(i + 1)));
            }
        }
    }

    private static void buildExceptionalEdges(StmtCFG cfg) {
        IR ir = cfg.getIR();
        ThrowResult throwResult = ir.getResult(ThrowAnalysis.ID);
        CatchResult catchResult = CatchAnalysis.analyze(ir, throwResult);
        ir.forEach(stmt -> {
            // build edges for implicit exceptions
            catchResult.getCaughtImplicitOf(stmt).forEachSet((catcher, exceptions) ->
                    cfg.inEdgesOf(stmt)
                            .filter(Predicate.not(Edge::isExceptional))
                            .map(Edge::getSource)
                            .forEach(pred ->
                                    cfg.addEdge(new ExceptionalEdge<>(
                                            Edge.Kind.CAUGHT_EXCEPTION,
                                            pred, catcher, exceptions))));
            Set<ClassType> uncaught = catchResult.getUncaughtImplicitOf(stmt);
            if (!uncaught.isEmpty()) {
                cfg.inEdgesOf(stmt)
                        .filter(Predicate.not(Edge::isExceptional))
                        .map(Edge::getSource)
                        .forEach(pred -> cfg.addEdge(
                                new ExceptionalEdge<>(
                                        Edge.Kind.UNCAUGHT_EXCEPTION,
                                        pred, cfg.getExit(), uncaught)));
            }
            // build edges for explicit exceptions
            if (stmt instanceof Throw || stmt instanceof Invoke) {
                catchResult.getCaughtExplicitOf(stmt).forEachSet((catcher, exceptions) ->
                        cfg.addEdge(new ExceptionalEdge<>(
                                Edge.Kind.CAUGHT_EXCEPTION,
                                stmt, catcher, exceptions))
                );
                Set<ClassType> uncaughtEx = catchResult.getUncaughtExplicitOf(stmt);
                if (!uncaughtEx.isEmpty()) {
                    cfg.addEdge(new ExceptionalEdge<>(
                            Edge.Kind.UNCAUGHT_EXCEPTION,
                            stmt, cfg.getExit(), uncaughtEx));
                }
            }
        });
    }
}
