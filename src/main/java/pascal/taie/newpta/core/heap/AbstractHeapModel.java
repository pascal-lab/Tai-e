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

package pascal.taie.newpta.core.heap;

import pascal.taie.ir.exp.ObjectExp;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.classes.StringReps;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;

import java.util.Map;

import static pascal.taie.java.classes.StringReps.THREAD;
import static pascal.taie.java.classes.StringReps.THREAD_GROUP;
import static pascal.taie.util.CollectionUtils.newMap;

/**
 * All heap models should inherit this class, and we can define
 * some uniform behaviors of heap modeling here.
 */
abstract class AbstractHeapModel implements HeapModel {

    private final TypeManager typeManager;

    private final Type string;

    private final Type stringBuilder;

    private final Type stringBuffer;

    private final Type throwable;

    /**
     * The merged object representing string constants.
     */
    private final MergedObj mergedSC;

    private final Map<Type, MergedObj> mergedObjs = newMap();

    // Special objects managed/created by Java runtime environment
    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    protected AbstractHeapModel(TypeManager typeManager) {
        this.typeManager = typeManager;
        string = typeManager.getClassType(StringReps.STRING);
        stringBuilder = typeManager.getClassType(StringReps.STRING_BUILDER);
        stringBuffer = typeManager.getClassType(StringReps.STRING_BUFFER);
        throwable = typeManager.getClassType(StringReps.THROWABLE);
        mergedSC = new MergedObj(string, "<Merged string constants>");

        mainThread = new EnvObj("<main-thread>",
                typeManager.getClassType(THREAD), null);
        systemThreadGroup = new EnvObj("<system-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        mainThreadGroup = new EnvObj("<main-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new EnvObj("<main-arguments>",
                stringArray, World.getMainMethod());
        mainArgsElem = new EnvObj("<main-arguments-element>",
                string, World.getMainMethod());
    }

    @Override
    public Obj getObj(ObjectExp exp) {
        Type type = exp.getType();
        Obj obj = exp.getObj();
        if (PTAOptions.get().isMergeStringConstants() &&
                exp instanceof StringLiteral) {
            mergedSC.addRepresentedObj(obj);
            return mergedSC;
        }
        if (PTAOptions.get().isMergeStringObjects() &&
                type.equals(string) &&
                !(exp instanceof StringLiteral)) {
            return getMergedObj(type, obj);
        }
        if (PTAOptions.get().isMergeStringBuilders()
                && (type.equals(stringBuilder) || type.equals(stringBuffer))) {
            return getMergedObj(type, obj);
        }
        if (PTAOptions.get().isMergeExceptionObjects()
                && typeManager.isSubtype(throwable, type)) {
            return getMergedObj(type, obj);
        }
        return doGetObj(exp);
    }

    /**
     * The method which controls the heap modeling for normal objects.
     */
    protected abstract Obj doGetObj(ObjectExp exp);

    /**
     * @param type the type of the objects to be merged
     * @param obj the object to be merged
     * @return the merged object
     */
    private Obj getMergedObj(Type type, Obj obj) {
        MergedObj mergedObj = mergedObjs.computeIfAbsent(
                type, (k) -> new MergedObj(type, "<Merged " + type + ">"));
        mergedObj.addRepresentedObj(obj);
        return mergedObj;
    }

    @Override
    public Obj getMainThread() {
        return mainThread;
    }

    @Override
    public Obj getSystemThreadGroup() {
        return systemThreadGroup;
    }

    @Override
    public Obj getMainThreadGroup() {
        return mainThreadGroup;
    }

    @Override
    public Obj getMainArgs() {
        return mainArgs;
    }

    @Override
    public Obj getMainArgsElem() {
        return mainArgsElem;
    }
}
