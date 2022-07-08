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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.util.Hashes;
import pascal.taie.util.graph.AbstractEdge;

public class PointerFlowEdge extends AbstractEdge<Pointer> {

    private final Kind kind;

    /**
     * Transfer function on this edge.
     */
    private final Transfer transfer;

    public PointerFlowEdge(Kind kind, Pointer source, Pointer target, Transfer transfer) {
        super(source, target);
        this.kind = kind;
        this.transfer = transfer;
    }

    public Kind getKind() {
        return kind;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerFlowEdge that = (PointerFlowEdge) o;
        return kind == that.kind &&
                source.equals(that.source) &&
                target.equals(that.target) &&
            transfer.equals(that.transfer);
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(kind, source, target, transfer);
    }

    @Override
    public String toString() {
        return "[" + kind + "]" + source + " -> " + target;
    }

    public enum Kind {
        LOCAL_ASSIGN,
        CAST,

        INSTANCE_LOAD,
        INSTANCE_STORE,

        ARRAY_LOAD,
        ARRAY_STORE,

        STATIC_LOAD,
        STATIC_STORE,

        PARAMETER_PASSING,
        RETURN,
    }
}
