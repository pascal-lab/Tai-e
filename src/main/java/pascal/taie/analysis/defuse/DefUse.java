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

package pascal.taie.analysis.defuse;

import pascal.taie.analysis.StmtResult;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.TwoKeyMultiMap;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the analysis result of {@link DefUseAnalysis}, i.e.,
 * both def-use chain and use-def chain.
 */
public class DefUse implements StmtResult<MultiMap<Var, Stmt>> {

    private static final String NULL_DEFS = "defs is null (not computed)" +
            " as it is disabled in def-use analysis";

    private static final String NULL_USES = "uses is null (not computed)" +
            " as it is disabled in def-use analysis";

    @Nullable
    private final TwoKeyMultiMap<Stmt, Var, Stmt> defs;

    @Nullable
    private final MultiMap<Stmt, Stmt> uses;

    DefUse(@Nullable TwoKeyMultiMap<Stmt, Var, Stmt> defs,
           @Nullable MultiMap<Stmt, Stmt> uses) {
        this.defs = defs;
        this.uses = uses;
    }

    /**
     * @return definitions of {@code var} at {@code stmt}.
     * If {@code var} is not used in {@code stmt} or it does not
     * have any definitions, an empty set is returned.
     */
    public Set<Stmt> getDefs(Stmt stmt, Var var) {
        Objects.requireNonNull(defs, NULL_DEFS);
        return defs.get(stmt, var);
    }

    /**
     * @return uses of the variable defined by {@code stmt}.
     * If {@code stmt} does not define any variable or the defined variable
     * does not have any uses, an empty Set is returned.
     */
    public Set<Stmt> getUses(Stmt stmt) {
        Objects.requireNonNull(uses, NULL_USES);
        return uses.get(stmt);
    }

    @Override
    public boolean isRelevant(Stmt stmt) {
        return true;
    }

    /**
     * {@link StmtResult} for def-use analysis. Note that this result only
     * contains use-def chain, and it is mainly for testing purpose.
     */
    @Override
    public MultiMap<Var, Stmt> getResult(Stmt stmt) {
        Objects.requireNonNull(defs, NULL_DEFS);
        return defs.get(stmt);
    }
}
