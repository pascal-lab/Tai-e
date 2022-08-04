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

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.HybridBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Map;
import java.util.Set;

class FieldPointsToGraph {

    private final TwoKeyMap<Obj, Field, Set<Obj>> fieldPointsTo = Maps.newTwoKeyMap();

    private final Set<Obj> objects;

    FieldPointsToGraph(PointerAnalysisResult pta) {
        initialize(pta);
        objects = CollectionUtils.toSet(pta.getObjects());
    }

    private void initialize(PointerAnalysisResult pta) {
        // build field points-to graph by examining points-to results
        // TODO - 1) eliminate redundant additions, points-to sets of some o.f
        //  may be added multiple times; 2) scan fields of all objects
        //  instead of only the loaded ones (?)
        Field.Factory factory = new Field.Factory();
        Indexer<Obj> objIndexer = pta.getObjectIndexer();
        for (Var var : pta.getVars()) {
            for (LoadField load : var.getLoadFields()) {
                if (pta.isConcerned(load.getRValue())) {
                    for (Obj baseObj : pta.getPointsToSet(var)) {
                        Field field = factory.get(load.getFieldRef().resolve());
                        fieldPointsTo.computeIfAbsent(baseObj, field,
                                        (b, f) -> new HybridBitSet<>(objIndexer, true))
                                .addAll(pta.getPointsToSet(load.getRValue()));
                    }
                }
            }
            for (LoadArray load : var.getLoadArrays()) {
                if (pta.isConcerned(load.getRValue())) {
                    for (Obj baseObj : pta.getPointsToSet(var)) {
                        Field field = factory.getArrayIndex();
                        fieldPointsTo.computeIfAbsent(baseObj, field,
                                        (b, f) -> new HybridBitSet<>(objIndexer, true))
                                .addAll(pta.getPointsToSet(load.getRValue()));
                    }
                }
            }
        }
    }

    Set<Obj> getObjects() {
        return objects;
    }

    Set<Field> dotFieldsOf(Obj baseObj) {
        return fieldPointsTo.getOrDefault(baseObj, Map.of()).keySet();
    }

    Set<Obj> pointsTo(Obj baseObj, Field field) {
        return fieldPointsTo.getOrDefault(baseObj, field, Set.of());
    }

    boolean hasPointer(Obj baseObj, Field field) {
        return fieldPointsTo.containsKey(baseObj, field);
    }
}
