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
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.SetEx;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CSMethodThrowResult {

    private final Supplier<SetEx<CSObj>> setFactory;

    private final Map<Stmt, SetEx<CSObj>> explicitExceptions;

    private final SetEx<CSObj> uncaughtExceptions;

    CSMethodThrowResult(Supplier<SetEx<CSObj>> setFactory) {
        this.setFactory = setFactory;
        explicitExceptions = Maps.newHybridMap();
        uncaughtExceptions = setFactory.get();
    }

    Set<CSObj> propagate(Stmt stmt, Set<CSObj> exceptions) {
        return explicitExceptions.computeIfAbsent(stmt, __ -> setFactory.get())
                .addAllDiff(exceptions);
    }

    void addUncaughtExceptions(Set<CSObj> exceptions) {
        uncaughtExceptions.addAll(exceptions);
    }

    Set<CSObj> mayThrowExplicitly(Stmt stmt) {
        Set<CSObj> result = explicitExceptions.get(stmt);
        return result != null ? result : Set.of();
    }

    Set<CSObj> mayThrowUncaught() {
        return Collections.unmodifiableSet(uncaughtExceptions);
    }
}
