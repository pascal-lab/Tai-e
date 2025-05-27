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

package pascal.taie.ir.exp;

import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of phi expression, e.g., φ(a, b).
 */
public class PhiExp implements RValue {

    private final List<Pair<Stmt, Var>> sourceAndVar;

    private final Type type;

    public static final Stmt METHOD_ENTRY = new Nop() {
        @Override
        public String toString() {
            return "METHOD_ENTRY";
        }
    };

    /**
     * @return the list of pairs of source statement and variable.
     * The source statement represents the source coming control flow
     * <p>
     * E.g., let {@code phi(s1:a, s2:b)} be the current phi expression.
     * <pre>
     * {@code
     * Block1 -> phi(s1:a, s2:b)
     * Block2 -> phi(s1:a, s2:b)
     * }
     * </pre>
     * {@code s1} will be the last statement of Block1, and {@code s2} will be the
     * last statement of Block2.
     */
    public List<Pair<Stmt, Var>> getSourceAndVar() {
        return sourceAndVar;
    }

    public PhiExp(List<Pair<Stmt, Var>> sourceAndVar, Type type) {
        this.sourceAndVar = sourceAndVar;
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Set<RValue> getUses() {
        return sourceAndVar
                .stream()
                .map(Pair::second)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        String repr;
        repr = sourceAndVar.stream()
                .map(p -> p.first() + ":" + p.second().toString())
                .collect(Collectors.joining(", "));
        return "Φ(" + repr + ")";
    }


}
