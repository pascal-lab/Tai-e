/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.heap;

import pascal.taie.java.TypeManager;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;
import pascal.taie.pta.ir.Allocation;
import pascal.taie.pta.ir.Obj;

import java.util.concurrent.ConcurrentMap;

import static pascal.taie.util.CollectionUtils.newConcurrentMap;

/**
 * All heap models should inherit this class, and we can define
 * some uniform behaviors of heap modeling here.
 */
abstract class AbstractHeapModel implements HeapModel {
    
    private final TypeManager typeManager;
    
    private final Type STRING;
    
    private final Type STRINGBUILDER;
    
    private final Type STRINGBUFFER;
    
    private final Type THROWABLE;
    /**
     * The merged object representing string constants.
     */
    private final MergedObj mergedSC;
    private final ConcurrentMap<Type, MergedObj> mergedObjs = newConcurrentMap();

    AbstractHeapModel(TypeManager typeManager) {
        this.typeManager = typeManager;
        STRING = typeManager.getClassType("java.lang.String");
        STRINGBUILDER = typeManager.getClassType("java.lang.StringBuilder");
        STRINGBUFFER = typeManager.getClassType("java.lang.StringBuffer");
        THROWABLE = typeManager.getClassType("java.lang.Throwable");
        mergedSC = new MergedObj(STRING, "<Merged string constants>");
    }

    @Override
    public Obj getObj(Allocation alloc) {
        Obj obj = alloc.getObject();
        Type type = obj.getType();
        if (PTAOptions.get().isMergeStringConstants()
                && obj.getKind() == Obj.Kind.STRING_CONSTANT) {
            // TODO: add represented objects optionally, as this affects
            //  the concurrent computation
            mergedSC.addRepresentedObj(obj);
            return mergedSC;
        }
        if (PTAOptions.get().isMergeStringObjects()
                && type.equals(STRING)
                && obj.getKind() != Obj.Kind.STRING_CONSTANT) {
            return getMergedObj(type, obj);
        }
        if (PTAOptions.get().isMergeStringBuilders()
                && (type.equals(STRINGBUILDER) || type.equals(STRINGBUFFER))) {
            return getMergedObj(type, obj);
        }
        if (PTAOptions.get().isMergeExceptionObjects()
                && typeManager.isSubtype(THROWABLE, type)) {
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
