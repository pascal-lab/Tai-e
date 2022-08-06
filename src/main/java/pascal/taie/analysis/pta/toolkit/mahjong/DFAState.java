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

package pascal.taie.analysis.pta.toolkit.mahjong;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class DFAState {

    private final Set<Obj> objs;

    private final Set<Type> output;

    private final ConcurrentMap<Field, DFAState> nextMap;

    private int hashCode = 0;

    DFAState(Set<Obj> objs, Set<Type> output) {
        this.objs = objs;
        this.output = output;
        this.nextMap = Maps.newConcurrentMap();
    }

    Set<Obj> getObjects() {
        return objs;
    }

    Set<Type> getOutput() {
        return output;
    }

    void addTransition(Field f, DFAState nextState) {
        nextMap.put(f, nextState);
    }

    Map<Field, DFAState> getNextMap() {
        return nextMap;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = objs.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DFAState anoDFAState)) {
            return false;
        }
        return getObjects().equals(anoDFAState.getObjects());
    }

    @Override
    public String toString() {
        return getObjects().stream()
                .map(Objects::toString)
                .collect(Collectors.toSet())
                .toString();
    }
}
