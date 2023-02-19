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

import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

class ExceptionalEdge<N> extends CFGEdge<N> {

    private final Set<ClassType> exceptions;

    ExceptionalEdge(CFGEdge.Kind kind, N source, N target,
                    Set<ClassType> exceptions) {
        super(kind, source, target);
        // other exception types might be added to this exceptional edge later,
        // thus we do not use unmodifiable set to store exception types
        this.exceptions = Sets.newHybridSet(exceptions);
    }

    void addExceptions(Collection<ClassType> exceptions) {
        this.exceptions.addAll(exceptions);
    }

    @Override
    public Set<ClassType> getExceptions() {
        return Collections.unmodifiableSet(exceptions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ExceptionalEdge<?> that = (ExceptionalEdge<?>) o;
        return exceptions.equals(that.exceptions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + exceptions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " with exceptions " + exceptions;
    }
}
