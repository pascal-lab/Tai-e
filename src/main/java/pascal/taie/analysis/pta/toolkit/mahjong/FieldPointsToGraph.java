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
import pascal.taie.language.classes.JField;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

class FieldPointsToGraph {

    private final Set<Obj> objects;

    private final ConcurrentMap<Obj, ConcurrentMap<Field, Set<Obj>>> fieldPointsTo;

    FieldPointsToGraph(PointerAnalysisResult pta) {
        objects = CollectionUtils.toSet(pta.getObjects());
        fieldPointsTo = Maps.newConcurrentMap();
        initialize(pta);
    }

    private void initialize(PointerAnalysisResult pta) {
        // build field points-to graph by examining points-to results
        // and scanning all the fields and array indices
        Field.Factory factory = new Field.Factory();
        pta.getInstanceFields().parallelStream()
                .filter(instanceField -> isConcerned(instanceField.getType()))
                .forEach(instanceField -> {
                    Obj baseObj = instanceField.getBase().getObject();
                    JField jField = instanceField.getField();
                    Set<Obj> pts = pta.getPointsToSet(baseObj, jField);
                    addFieldPointsTo(baseObj, factory.get(jField), pts);
                });
        pta.getArrayIndexes().parallelStream()
                .filter(arrayIndex -> isConcerned(arrayIndex.getType()))
                .forEach(arrayIndex -> {
                    Obj array = arrayIndex.getArray().getObject();
                    Set<Obj> pts = pta.getPointsToSet(array);
                    addFieldPointsTo(array, factory.getArrayIndex(), pts);
                });
    }

    private static boolean isConcerned(Type type) {
        return type instanceof ReferenceType && !(type instanceof NullType);
    }

    private void addFieldPointsTo(Obj baseObj, Field field, Set<Obj> pts) {
        fieldPointsTo.computeIfAbsent(baseObj, o -> Maps.newConcurrentMap())
                .computeIfAbsent(field, f -> Sets.newConcurrentSet())
                .addAll(pts);
    }

    Set<Obj> getObjects() {
        return objects;
    }

    Set<Field> dotFieldsOf(Obj baseObj) {
        ConcurrentMap<Field, Set<Obj>> fp = fieldPointsTo.get(baseObj);
        return fp != null ? fp.keySet() : Set.of();
    }

    Set<Obj> pointsTo(Obj baseObj, Field field) {
        ConcurrentMap<Field, Set<Obj>> fp = fieldPointsTo.get(baseObj);
        return fp != null ? fp.getOrDefault(field, Set.of()) : Set.of();
    }

    boolean hasPointer(Obj baseObj, Field field) {
        ConcurrentMap<Field, Set<Obj>> fp = fieldPointsTo.get(baseObj);
        return fp != null && fp.containsKey(field);
    }
}
