/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.core.heap;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import bamboo.pta.statement.Allocation;

import java.util.HashMap;
import java.util.Map;

/**
 * All heap models should inherit this class, and we can define
 * some uniform behaviors of heap modeling here.
 */
abstract class AbstractHeapModel implements HeapModel {

    private final Type STRING;
    private final Type STRINGBUILDER;
    private final Type STRINGBUFFER;
    private final Map<Type, MergedObj> mergedObjs = new HashMap<>();

    AbstractHeapModel(ProgramManager pm) {
        STRING = pm.getUniqueTypeByName("java.lang.String");
        STRINGBUILDER = pm.getUniqueTypeByName("java.lang.StringBuilder");
        STRINGBUFFER = pm.getUniqueTypeByName("java.lang.StringBuffer");
    }

    @Override
    public Obj getObj(Allocation alloc) {
        return doGetObj(alloc);
    }

    protected abstract Obj doGetObj(Allocation alloc);

    private Obj getMergedObj(Type type, Obj obj) {
        MergedObj mergedObj = mergedObjs.computeIfAbsent(
                type, (k) -> new MergedObj(type));
        mergedObj.addRepresentedObj(obj);
        return mergedObj;
    }
}
