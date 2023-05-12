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

import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * CFG with {@code Stmt} as nodes. This class maintains a mapping between
 * indexes and nodes in this graph as follows:
 * <ul>
 *     <li>Entry node is mapped to index 0</li>
 *     <li>Exit node is mapped to index <code>ir.getStmts().size() + 1</code></li>
 *     <li>Other nodes (stmts) are mapped to index <code>stmt.getIndex() + 1</code></li>
 * </ul>
 * Basically, it vacates index 0 for entry node, shifts stmts in IR by 1,
 * and appends exit node at last.
 */
class StmtCFG extends AbstractCFG<Stmt> {

    public StmtCFG(IR ir) {
        super(ir);
    }

    @Override
    public int getIndex(Stmt stmt) {
        if (isEntry(stmt)) {
            return 0;
        } else if (isExit(stmt)) {
            return ir.getStmts().size() + 1;
        } else {
            return stmt.getIndex() + 1;
        }
    }

    @Override
    public Stmt getNode(int index) {
        if (index == 0) {
            return getEntry();
        } else if (index == ir.getStmts().size() + 1) {
            return getExit();
        } else {
            return ir.getStmt(index - 1);
        }
    }

    @Override
    public Set<Stmt> getNodes() {
        // keep nodes sorted to ease debugging
        Set<Stmt> stmts = Sets.newOrderedSet(Comparator.comparing(this::getIndex));
        stmts.addAll(super.getNodes());
        return Collections.unmodifiableSet(stmts);
    }
}
