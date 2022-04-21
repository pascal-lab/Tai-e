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

package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Collections;
import java.util.Set;

import static pascal.taie.util.collection.Maps.newHybridMap;
import static pascal.taie.util.collection.Sets.newHybridSet;

public class MethodThrowResult {

    private final JMethod method;

    private final MultiMap<Stmt, Obj> explicitExceptions
            = Maps.newMultiMap(newHybridMap());

    private final Set<Obj> uncaughtExceptions = newHybridSet();

    public MethodThrowResult(JMethod method) {
        this.method = method;
    }

    public Set<Obj> mayThrowExplicitly(Stmt stmt) {
        return explicitExceptions.get(stmt);
    }

    public Set<Obj> mayThrowUncaught() {
        return Collections.unmodifiableSet(uncaughtExceptions);
    }

    void addCSMethodThrowResult(CSMethodThrowResult csMethodThrowResult) {
        for (Stmt stmt : method.getIR()) {
            csMethodThrowResult.mayThrowExplicitly(stmt)
                    .stream()
                    .map(CSObj::getObject)
                    .forEach(exception ->
                            explicitExceptions.put(stmt, exception));
        }
        csMethodThrowResult.mayThrowUncaught()
                .stream()
                .map(CSObj::getObject)
                .forEach(uncaughtExceptions::add);
    }

    @Override
    public String toString() {
        return "MethodThrowResult{" +
                "method=" + method +
                ", explicitExceptions=" + explicitExceptions +
                ", uncaughtExceptions=" + uncaughtExceptions +
                '}';
    }
}
