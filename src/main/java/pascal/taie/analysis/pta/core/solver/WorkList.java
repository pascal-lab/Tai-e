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
import pascal.taie.analysis.pta.pts.PointsToSet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Represents work list in pointer analysis.
 */
final class WorkList {

    private final Map<Pointer, PointsToSet> entries = new LinkedHashMap<>();

    void addEntry(Pointer pointer, PointsToSet pointsToSet) {
        PointsToSet set = entries.get(pointer);
        if (set != null) {
            set.addAll(pointsToSet);
        } else {
            entries.put(pointer, pointsToSet.copy());
        }
    }

    Entry pollEntry() {
        if (entries.isEmpty()) {
            throw new NoSuchElementException();
        }
        var it = entries.entrySet().iterator();
        var e = it.next();
        it.remove();
        return new Entry(e.getKey(), e.getValue());
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    record Entry(Pointer pointer, PointsToSet pointsToSet) {
    }
}
