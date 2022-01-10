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

import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Stmt;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

class StmtCFG extends AbstractCFG<Stmt> {

    public StmtCFG(IR ir) {
        super(ir);
    }

    @Override
    public Set<Stmt> getNodes() {
        // sort nodes to ease debugging
        Comparator<Stmt> orderer = (n1, n2) -> {
            if (n1.equals(n2)) {
                return 0;
            } else if (isEntry(n1) || isExit(n2)) {
                return -1;
            } else if (isExit(n1) || isEntry(n2)) {
                return 1;
            } else {
                return n1.getIndex() - n2.getIndex();
            }
        };
        Set<Stmt> stmts = new TreeSet<>(orderer);
        stmts.addAll(super.getNodes());
        return Collections.unmodifiableSet(stmts);
    }
}
