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

import java.util.stream.Stream;

class StmtCFG extends AbstractCFG<Stmt> {

    public StmtCFG(IR ir) {
        super(ir);
    }

    @Override
    public Stream<Stmt> nodes() {
        // sort nodes to ease debugging
        return super.nodes()
                .sorted((n1, n2) -> {
                    if (n1.equals(n2)) {
                        return 0;
                    } else if (isEntry(n1) || isExit(n2)) {
                        return -1;
                    } else if (isExit(n1) || isEntry(n2)) {
                        return 1;
                    } else {
                        return n1.getIndex() - n2.getIndex();
                    }
                });
    }
}
