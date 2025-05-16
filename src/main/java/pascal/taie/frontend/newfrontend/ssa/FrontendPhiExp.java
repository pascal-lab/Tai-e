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
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of phi expression, e.g., φ(a, b).
 * Different from {@link pascal.taie.ir.exp.PhiExp}, this class is used
 * to represent the phi expression in the frontend.
 *
 * <p>
 * The main difference is that this class use {@link IBasicBlock} as the
 * corresponding block, while {@link pascal.taie.ir.exp.PhiExp}
 * uses {@link pascal.taie.ir.stmt.Stmt} as the corresponding
 * block.
 * </p>
 */
public class FrontendPhiExp implements RValue {
    private final List<Pair<Var, IBasicBlock>> usesAndInBlocks = new ArrayList<>();

    public void addUseAndCorrespondingBlocks(Var v, IBasicBlock block) {
        usesAndInBlocks.add(new Pair<>(v, block));
    }

    List<Pair<Var, IBasicBlock>> getUsesAndInBlocks() {
        return usesAndInBlocks;
    }

    @Override
    public String toString() {
        String repr;
        repr = usesAndInBlocks.stream()
                .map(p -> p.first().toString())
                .collect(Collectors.joining(", "));
        return "Φ(" + repr + ")";
    }

    public Var findVar(IBasicBlock block) {
        for (Pair<Var, IBasicBlock> pair : usesAndInBlocks) {
            if (pair.second() == block) {
                return pair.first();
            }
        }
        throw new IllegalArgumentException("No such block");
    }

    @Override
    public Type getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<RValue> getUses() {
        return usesAndInBlocks
                .stream()
                .map(Pair::first)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }
}
