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

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.Predicate;

class NFA {

    private static final Descriptor DEAD_STATE_DESC = () -> "DeadState";

    private static final Obj DEAD_STATE = new MockObj(
            DEAD_STATE_DESC, null, null, null, false);

    private final Obj q0;

    private final FieldPointsToGraph fpg;

    NFA(Obj q0, FieldPointsToGraph fpg) {
        this.q0 = q0;
        this.fpg = fpg;
    }

    /**
     * This method on-the-fly computes set of states.
     *
     * @return Set of states (dead state is excluded).
     */
    Set<Obj> getStates() {
        Set<Obj> states = Sets.newSet();
        Deque<Obj> stack = new ArrayDeque<>();
        stack.push(q0);
        while (!stack.isEmpty()) {
            Obj s = stack.pop();
            if (!states.contains(s)) {
                states.add(s);
                outEdgesOf(s).forEach(field ->
                        nextStates(s, field)
                                .stream()
                                .filter(Predicate.not(states::contains))
                                .forEach(stack::push));
            }
        }
        return states;
    }

    Obj getStartState() {
        return q0;
    }

    Set<Obj> nextStates(Obj obj, Field f) {
        if (isDeadState(obj) || !fpg.hasPointer(obj, f)) {
            return Set.of(DEAD_STATE);
        } else {
            return fpg.pointsTo(obj, f);
        }
    }

    Set<Field> outEdgesOf(Obj obj) {
        if (isDeadState(obj)) {
            return Set.of();
        } else {
            return fpg.dotFieldsOf(obj);
        }
    }

    Type outputOf(Obj obj) {
        return obj.getType();
    }

    static Obj getDeadState() {
        return DEAD_STATE;
    }

    static boolean isDeadState(Obj obj) {
        return DEAD_STATE == obj;
    }
}
