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

package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.ir.exp.PhiExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Used to convert a {@link FrontendPhiExp} to a {@link PhiExp}
 */
public class PhiResolver<T extends IBasicBlock> {

    private final IndexedGraph<T> graph;

    public PhiResolver(IndexedGraph<T> graph) {
        this.graph = graph;
    }

    public List<Pair<Stmt, Var>> resolvePhi(FrontendPhiExp exp) {
        var usesAndInBlocks = exp.getUsesAndInBlocks();
        List<Pair<Stmt, Var>> sourceAndVar = new ArrayList<>(usesAndInBlocks.size());
        for (Pair<Var, IBasicBlock> p : usesAndInBlocks) {
            Var v = p.first();
            IBasicBlock b = p.second();
            Stmt stmt = getSourceIndex((T) b);
            Pair<Stmt, Var> np = new Pair<>(stmt, v);
            if (!sourceAndVar.contains(np)) {
                sourceAndVar.add(np);
            }
        }
        sourceAndVar.sort(Comparator.comparing(p -> p.first().getIndex()));
        return Collections.unmodifiableList(sourceAndVar);
    }

    private Stmt getSourceIndex(T block) {
        if (block == null) {
            return PhiExp.METHOD_ENTRY;
        }
        if (!block.getStmts().isEmpty()) {
            List<Stmt> stmts = block.getStmts();
            return stmts.get(stmts.size() - 1);
        } else {
            /*
            The block is within a try block, and the bytecode in it is translated as side
            effect. So we have to find the next non-empty block and get its first stmt.
            */
            while (block.getStmts().isEmpty()) {
                List<T> outEdges = graph.normalOutEdges(block);
                assert outEdges.size() == 1;
                block = outEdges.get(0);
            }
            return block.getStmts().get(0);
        }
    }
}
