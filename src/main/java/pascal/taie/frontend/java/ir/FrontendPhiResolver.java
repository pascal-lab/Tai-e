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

package pascal.taie.frontend.java.ir;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import pascal.taie.frontend.java.ir.ssa.FrontendPhiExp;
import pascal.taie.ir.exp.PhiExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

/**
 * Converts a {@link FrontendPhiExp} to a {@link PhiExp}.
 * <p>
 * This resolver maps each variable-block pair in the frontend phi expression
 * to a corresponding source statement, producing the final phi expression
 * representation used in the IR.
 */
class FrontendPhiResolver {

    private final BytecodeCFG cfg;

    FrontendPhiResolver(BytecodeCFG cfg) {
        this.cfg = cfg;
    }

    /**
     * Resolves a frontend phi expression to a list of (source statement, variable) pairs.
     * <p>
     * Each pair represents a phi operand: the variable value and the statement
     * from which control flow originates.
     *
     * @param phiExp the frontend phi expression to resolve
     * @return an unmodifiable list of (Stmt, Var) pairs, sorted by statement index
     */
    List<Pair<Stmt, Var>> resolvePhi(FrontendPhiExp phiExp) {
        // Use LinkedHashSet to deduplicate while preserving insertion order
        Set<Pair<Stmt, Var>> resolvedPairs = Sets.newLinkedSet();

        for (Pair<Var, BytecodeBlock> useAndBlock : phiExp.getUsesAndInBlocks()) {
            Var var = useAndBlock.first();
            BytecodeBlock block = useAndBlock.second();
            Stmt sourceStmt = resolveSourceStmt(block);
            resolvedPairs.add(new Pair<>(sourceStmt, var));
        }

        return resolvedPairs.stream()
                .sorted(Comparator.comparingInt(pair -> pair.first().getIndex()))
                .toList();
    }

    /**
     * Resolves the source statement for a given block.
     * <p>
     * The source statement is:
     * <ul>
     *   <li>{@link PhiExp#METHOD_ENTRY} if the block is null (method entry)</li>
     *   <li>The last statement of the block if the block is non-empty</li>
     *   <li>The first statement of the next non-empty successor block
     *       if the block is empty (e.g., side-effect-only bytecode in try blocks)</li>
     * </ul>
     *
     * @param block the bytecode block to resolve
     * @return the source statement representing the control flow origin
     */
    private Stmt resolveSourceStmt(BytecodeBlock block) {
        if (block == null) {
            return PhiExp.METHOD_ENTRY;
        }
        Stmt lastStmt = block.getLastStmt();
        if (lastStmt != null) {
            return lastStmt;
        }
        return getFirstStmtOfNextNonEmptyBlock(block);
    }

    /**
     * Finds the first statement of the next non-empty successor block.
     * <p>
     * This handles cases where a block within a try block has its bytecode
     * translated as a side effect, leaving the block empty. We traverse
     * normal successors until we find a non-empty block.
     *
     * @param emptyBlock the empty block to start from
     * @return the first statement of the next non-empty successor block
     */
    private Stmt getFirstStmtOfNextNonEmptyBlock(BytecodeBlock emptyBlock) {
        BytecodeBlock current = emptyBlock;
        while (current.getStmts().isEmpty()) {
            List<BytecodeBlock> successors = cfg.getNormalSuccsOf(current);
            assert successors.size() == 1 :
                    "Empty block should have exactly one normal successor";
            current = successors.get(0);
        }
        return current.getStmts().get(0);
    }
}
