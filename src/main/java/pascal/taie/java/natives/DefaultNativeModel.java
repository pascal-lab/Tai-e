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

package pascal.taie.java.natives;

import pascal.taie.ir.NewIR;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.newpta.core.heap.EnvObj;
import pascal.taie.newpta.core.heap.Obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static pascal.taie.java.classes.StringReps.OBJECT;
import static pascal.taie.java.classes.StringReps.STRING;
import static pascal.taie.java.classes.StringReps.THREAD;
import static pascal.taie.java.classes.StringReps.THREAD_GROUP;
import static pascal.taie.util.CollectionUtils.newMap;

public class DefaultNativeModel implements NativeModel {

    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    private final TypeManager typeManager;

    private final ClassHierarchy hierarchy;

    private final Map<JMethod, Function<JMethod, NewIR>> models = newMap();

    public DefaultNativeModel(TypeManager typeManager,
                              ClassHierarchy hierarchy) {
        mainThread = new EnvObj("<main-thread>",
                typeManager.getClassType(THREAD), null);
        systemThreadGroup = new EnvObj("<system-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        mainThreadGroup = new EnvObj("<main-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        Type string = typeManager.getClassType(STRING);
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new EnvObj("<main-arguments>",
                stringArray, World.getMainMethod());
        mainArgsElem = new EnvObj("<main-arguments-element>",
                string, World.getMainMethod());
        this.typeManager = typeManager;
        this.hierarchy = hierarchy;
        initModels();
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

    @Override
    public NewIR buildNativeIR(JMethod method) {
        return models.getOrDefault(method,
                m -> new NativeIRBuilder(method).buildEmpty())
                .apply(method);
    }

    private void initModels() {
        // --------------------------------------------------------------------
        // java.lang.Object
        // --------------------------------------------------------------------
        // <java.lang.Object: java.lang.Object clone()>
        // TODO: could throw CloneNotSupportedException
        // TODO: should check if the object is Cloneable.
        // TODO: should return a clone of the heap allocation (not
        //  identity). The behaviour implemented here is based on Soot.
        register("<java.lang.Object: java.lang.Object clone()>", m -> {
            NativeIRBuilder builder = new NativeIRBuilder(m);
            List<Stmt> stmts = new ArrayList<>();
            stmts.add(new Copy(builder.getReturnVar(), builder.getThisVar()));
            stmts.add(new Return(builder.getReturnVar()));
            return builder.build(stmts);
        });

        // --------------------------------------------------------------------
        // java.lang.System
        // --------------------------------------------------------------------
        // <java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
        register("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", m -> {
            NativeIRBuilder builder = new NativeIRBuilder(m);
            Var src = builder.getParam(0);
            Var index = builder.getParam(1); // TODO: iterate indexes?
            Var dest = builder.getParam(2);
            Type objType = typeManager.getClassType(OBJECT);
            Type arrayType = typeManager.getArrayType(objType, 1);
            Var srcArray = builder.newTempVar(arrayType);
            Var destArray = builder.newTempVar(arrayType);
            Var temp = builder.newTempVar(objType);
            // src/dest may point to non-array objects due to imprecision
            // of pointer analysis, thus we add cast statements to filter
            // out load/store operations on non-array objects.
            List<Stmt> stmts = new ArrayList<>();
            stmts.add(new Cast(srcArray, new CastExp(src, arrayType)));
            stmts.add(new Cast(destArray, new CastExp(dest, arrayType)));
            stmts.add(new LoadArray(temp, new ArrayAccess(srcArray, index)));
            stmts.add(new StoreArray(new ArrayAccess(destArray, index), temp));
            stmts.add(new Return());
            return builder.build(stmts);
        });
    }

    private void register(String methodSig, Function<JMethod, NewIR> model) {
        JMethod method = hierarchy.getJREMethod(methodSig);
        models.put(method, model);
    }
}
