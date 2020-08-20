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
import bamboo.pta.options.Options;
import bamboo.pta.statement.Allocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All heap models should inherit this class, and we can define
 * some uniform behaviors of heap modeling here.
 */
abstract class AbstractHeapModel implements HeapModel {

    private final ProgramManager pm;

    private final Type STRING;
    private final Type STRINGBUILDER;
    private final Type STRINGBUFFER;
    private final Type THROWABLE;
    /**
     * The merged object representing string constants.
     */
    private final MergedObj mergedSC;
    private final Map<Type, MergedObj> mergedObjs = new ConcurrentHashMap<>();

    AbstractHeapModel(ProgramManager pm) {
        this.pm = pm;
        STRING = pm.getUniqueTypeByName("java.lang.String");
        STRINGBUILDER = pm.getUniqueTypeByName("java.lang.StringBuilder");
        STRINGBUFFER = pm.getUniqueTypeByName("java.lang.StringBuffer");
        THROWABLE = pm.getUniqueTypeByName("java.lang.Throwable");
        mergedSC = new MergedObj(STRING, "<Merged string constants>");
    }

    @Override
    public Obj getObj(Allocation alloc) {
        Obj obj = alloc.getObject();
        Type type = obj.getType();
        if (Options.get().isMergeStringConstants()
                && obj.getKind() == Obj.Kind.STRING_CONSTANT) {
            // TODO: add represented objects optionally, as this affects
            //  the concurrent computation
            mergedSC.addRepresentedObj(obj);
            return mergedSC;
        }
        if (Options.get().isMergeStringObjects()
                && type.equals(STRING)
                && obj.getKind() != Obj.Kind.STRING_CONSTANT) {
            return getMergedObj(type, obj);
        }
        if (Options.get().isMergeStringBuilders()
                && (type.equals(STRINGBUILDER) || type.equals(STRINGBUFFER))) {
            return getMergedObj(type, obj);
        }
        if (Options.get().isMergeExceptionObjects()
                && pm.isSubtype(THROWABLE, type)) {
            return getMergedObj(type, obj);
        }
        return doGetObj(alloc);
    }

    /**
     * The method which controls the heap modeling for normal objects.
     */
    protected abstract Obj doGetObj(Allocation alloc);

    /**
     * @param type the type of the objects to be merged
     * @param obj the object to be merged
     * @return the merged object
     */
    private Obj getMergedObj(Type type, Obj obj) {
        MergedObj mergedObj = mergedObjs.computeIfAbsent(
                type, (k) -> new MergedObj(type, "<Merged " + type + ">"));
        // TODO: add represented objects optionally, as this affects
        //  the concurrent computation
        mergedObj.addRepresentedObj(obj);
        return mergedObj;
    }
}
