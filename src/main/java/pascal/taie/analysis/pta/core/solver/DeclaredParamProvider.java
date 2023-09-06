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

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.TwoKeyMultiMap;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * This {@link ParamProvider} creates parameter objects of the declared types,
 * and the objects pointed to by fields of parameter objects,
 * as well as elements of array objects. This class ignores
 * non-instantiable types, i.e., primitive types and abstract classes.
 */
public class DeclaredParamProvider implements ParamProvider {

    /**
     * Special index representing "this" variable.
     */
    private static final int THIS_INDEX = -1;

    /**
     * Represents combination of a method and a parameter index.
     *
     * @param method the entry method
     * @param index  the index of the parameter
     */
    private record MethodParam(JMethod method, int index) {

        @Override
        public String toString() {
            return "MethodParam{" + method + '/' +
                    (index == THIS_INDEX ? "this" : index) + '}';
        }
    }

    @Nullable
    private Obj thisObj;

    private Obj[] paramObjs;

    private TwoKeyMultiMap<Obj, JField, Obj> fieldObjs;

    private MultiMap<Obj, Obj> arrayObjs;

    /**
     * @param method    the entry method.
     * @param heapModel the model for generating mock objects.
     */
    public DeclaredParamProvider(JMethod method, HeapModel heapModel) {
        this(method, heapModel, 0);
    }

    /**
     * @param method    the entry method.
     * @param heapModel the model for generating mock objects.
     * @param k         level of field/array accesses. If this is not 0,
     *                  the provider generates objects recursively along
     *                  k field/array accesses.
     */
    public DeclaredParamProvider(JMethod method, HeapModel heapModel, int k) {
        generateObjs(method, heapModel, k);
    }

    private void generateObjs(JMethod method, HeapModel heapModel, int k) {
        Deque<Pair<Obj, Integer>> queue = new ArrayDeque<>();
        // generate this (receiver) object
        if (!method.isStatic() && !method.getDeclaringClass().isAbstract()) {
            thisObj = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                    new MethodParam(method, THIS_INDEX),
                    method.getDeclaringClass().getType(), method);
            queue.add(new Pair<>(thisObj, 0));
        }
        // generate parameter objects
        paramObjs = new Obj[method.getParamCount()];
        for (int i = 0; i < method.getParamCount(); ++i) {
            Type paramType = method.getParamType(i);
            if (isInstantiable(paramType)) {
                paramObjs[i] = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                        new MethodParam(method, i), paramType, method);
                queue.add(new Pair<>(paramObjs[i], 0));
            }
        }
        // generate k-level field and array objects by a level-order traversal
        fieldObjs = Maps.newTwoKeyMultiMap();
        arrayObjs = Maps.newMultiMap();
        while (!queue.isEmpty()) {
            Pair<Obj, Integer> pair = queue.pop();
            Obj base = pair.first();
            int level = pair.second();
            if (level < k) {
                Type type = base.getType();
                if (type instanceof ClassType cType) {
                    for (JField field : cType.getJClass().getDeclaredFields()) {
                        Type fieldType = field.getType();
                        if (isInstantiable(fieldType)) {
                            Obj obj = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                                    base.getAllocation() + "." + field.getName(),
                                    fieldType, method);
                            fieldObjs.put(base, field, obj);
                            queue.add(new Pair<>(obj, level + 1));
                        }
                    }
                } else if (type instanceof ArrayType aType) {
                    Type elemType = aType.elementType();
                    if (isInstantiable(elemType)) {
                        Obj elem = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                                base.getAllocation() + "[*]",
                                elemType, method);
                        arrayObjs.put(base, elem);
                        queue.add(new Pair<>(elem, level + 1));
                    }
                }
            }
        }
    }

    private static boolean isInstantiable(Type type) {
        return (type instanceof ClassType cType && !cType.getJClass().isAbstract())
                || type instanceof ArrayType;
    }

    @Override
    public Set<Obj> getThisObjs() {
        return thisObj != null ? Set.of(thisObj) : Set.of();
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        return paramObjs[i] != null ? Set.of(paramObjs[i]) : Set.of();
    }

    @Override
    public TwoKeyMultiMap<Obj, JField, Obj> getFieldObjs() {
        return Maps.unmodifiableTwoKeyMultiMap(fieldObjs);
    }

    @Override
    public MultiMap<Obj, Obj> getArrayObjs() {
        return Maps.unmodifiableMultiMap(arrayObjs);
    }
}
