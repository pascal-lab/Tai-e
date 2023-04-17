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

package pascal.taie.util.graph;

import pascal.taie.util.Hashes;

public abstract class AbstractEdge<N> implements Edge<N> {

    /**
     * The source node of the edge.
     */
    protected final N source;

    /**
     * The target node of the edge.
     */
    protected final N target;

    protected AbstractEdge(N source, N target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public N source() {
        return source;
    }

    @Override
    public N target() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractEdge<?> edge = (AbstractEdge<?>) o;
        return source.equals(edge.source) && target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(source, target);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{" + source + " -> " + target + '}';
    }
}
