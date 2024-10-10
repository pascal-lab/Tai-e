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

package pascal.taie.frontend.newfrontend.report;

import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;

public class VarReporter {
    static VarReporter instance = new VarReporter();

    final List<Pair<Integer, Integer>> res = new ArrayList<>();

    public static VarReporter get() {
        return instance;
    }

    public void report(int maxLocal, int varSize) {
        synchronized (res) {
            res.add(new Pair<>(maxLocal, varSize));
        }
    }

    public double getRatio() {
        long allMaxLocal = 0;
        long allVarSize = 0;
        for (var i : res) {
            allMaxLocal += i.first();
            allVarSize += i.second();
        }
        return allVarSize / (double) allMaxLocal;
    }
}
