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

package pascal.taie.analysis.sideeffect;

import pascal.taie.analysis.StmtResult;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents result of side-effect analysis.
 */
public class SideEffect implements StmtResult<Set<Obj>> {

    /**
     * Maps from a method to all objects directly or indirectly modified by it.
     */
    private final Map<JMethod, Set<Obj>> methodMods;

    /**
     * Maps from a stmt to the objects directly modified by it.
     */
    private final Map<Stmt, Set<Obj>> stmtDirectMods;

    private final CallGraph<Invoke, JMethod> callGraph;

    SideEffect(Map<JMethod, Set<Obj>> methodMods,
               Map<Stmt, Set<Obj>> stmtDirectMods,
               CallGraph<Invoke, JMethod> callGraph) {
        this.methodMods = methodMods;
        this.stmtDirectMods = stmtDirectMods;
        this.callGraph = callGraph;
    }

    /**
     * @return set of objects that may be modified by given method.
     */
    public Set<Obj> getModifiedObjects(JMethod method) {
        return methodMods.getOrDefault(method, Set.of());
    }

    /**
     * @return set of objects that may be modified by given stmt.
     */
    public Set<Obj> getModifiedObjects(Stmt stmt) {
        if (stmt instanceof Invoke invoke) {
            // to save space, we compute modified objects of
            // Invoke stmt on demand, and do not cache them
            return callGraph.getCalleesOf(invoke)
                    .stream()
                    .map(this::getModifiedObjects)
                    .flatMap(Set::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return stmtDirectMods.getOrDefault(stmt, Set.of());
    }

    /**
     * @return {@code true} if given method does not modify any objects.
     */
    public boolean isPure(JMethod method) {
        return !methodMods.containsKey(method);
    }

    @Override
    public boolean isRelevant(Stmt stmt) {
        return stmt instanceof Invoke ||
                stmt instanceof StoreArray ||
                (stmt instanceof StoreField storeField &&
                        !storeField.isStatic());
    }

    @Override
    public Set<Obj> getResult(Stmt stmt) {
        return getModifiedObjects(stmt);
    }
}
