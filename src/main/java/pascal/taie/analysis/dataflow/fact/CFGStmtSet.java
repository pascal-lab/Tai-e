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

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.AbstractBitSet;

/**
 * Represents set of statements in a {@link CFG}.
 */
public class CFGStmtSet extends AbstractBitSet<Stmt> {

    private final CFG<Stmt> cfg;

    public CFGStmtSet(CFG<Stmt> cfg) {
        this.cfg = cfg;
    }

    public CFGStmtSet(CFGStmtSet set) {
        super(set);
        this.cfg = set.cfg;
    }

    @Override
    public CFGStmtSet copy() {
        return new CFGStmtSet(this);
    }

    @Override
    protected Object getContext() {
        return cfg;
    }

    @Override
    protected int getIndex(Stmt stmt) throws IllegalArgumentException {
        return stmt.getIndex();
    }

    /**
     * Maps an index to corresponding {@link Stmt}.
     * This implementation is not elegant as the indexes of entry and exit
     * Stmts follow the last Stmt in the IR.
     * @see pascal.taie.analysis.graph.cfg.CFGBuilder
     */
    @Override
    protected Stmt getElement(int index) throws IllegalArgumentException {
        int nStmts = cfg.getIR().getStmts().size();
        if (index == nStmts) {
            return cfg.getEntry();
        } else if (index == nStmts + 1) {
            return cfg.getExit();
        } else {
            return cfg.getIR().getStmt(index);
        }
    }
}
