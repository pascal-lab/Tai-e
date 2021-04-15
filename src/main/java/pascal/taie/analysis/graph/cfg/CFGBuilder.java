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

import pascal.taie.analysis.exception.ThrowAnalysis;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;

public class CFGBuilder {

    private final ThrowAnalysis throwAnalysis;

    public CFGBuilder(ThrowAnalysis throwAnalysis) {
        this.throwAnalysis = throwAnalysis;
    }

    CFG<Stmt> build(IR ir) {
        StmtCFG cfg = new StmtCFG(ir);
        cfg.setEntry(new Nop());
        cfg.setExit(new Nop());
        buildNormalEdges(cfg);
        buildExceptionalEdges(cfg, throwAnalysis);
        return cfg;
    }

    private static void buildNormalEdges(StmtCFG cfg) {
        IR ir = cfg.getIR();
        cfg.addEdge(new Edge<>(Edge.Kind.ENTRY, cfg.getEntry(), ir.getStmt(0)));
        for (int i = 0; i < ir.getStmts().size(); ++i) {
            Stmt curr = ir.getStmt(i);
            if (curr instanceof Goto) {
                cfg.addEdge(new Edge<>(Edge.Kind.GOTO,
                        curr, ((Goto) curr).getTarget()));
            } if (curr instanceof If) {
                cfg.addEdge(new Edge<>(Edge.Kind.IF_TRUE,
                        curr, ((If) curr).getTarget()));
                cfg.addEdge(new Edge<>(Edge.Kind.IF_FALSE,
                        curr, ir.getStmt(i + 1)));
            } else if (curr instanceof SwitchStmt) {
                SwitchStmt switchStmt = (SwitchStmt) curr;
                switchStmt.getCaseTargets().forEach(pair -> {
                    int caseValue = pair.getFirst();
                    Stmt target = pair.getSecond();
                    cfg.addEdge(new SwitchCaseEdge<>(
                            switchStmt, target, caseValue));
                });
                cfg.addEdge(new Edge<>(Edge.Kind.SWITCH_DEFAULT,
                        switchStmt, switchStmt.getDefaultTarget()));
            } else if (curr instanceof Return) {
                cfg.addEdge(new Edge<>(Edge.Kind.RETURN, curr, cfg.getExit()));
            } else if (curr.canFallThrough()) {
                cfg.addEdge(new Edge<>(Edge.Kind.FALL_THROUGH,
                        curr, ir.getStmt(i + 1)));
            }
        }
    }

    private static void buildExceptionalEdges(StmtCFG cfg, ThrowAnalysis throwAnalysis) {
    }
}
