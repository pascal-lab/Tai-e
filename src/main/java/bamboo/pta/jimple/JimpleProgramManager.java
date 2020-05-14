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

package bamboo.pta.jimple;

import bamboo.pta.analysis.ProgramManager;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.SpecialInvokeExpr;

import java.util.Collection;
import java.util.Collections;

/**
 * Interface between soot and pointer analysis.
 */
public class JimpleProgramManager implements ProgramManager {

    private FastHierarchy hierarchy = Scene.v().getOrMakeFastHierarchy();

    private ElementManager elementManager = new ElementManager();

    @Override
    public Collection<Method> getEntryMethods() {
        return Collections.singleton(
                elementManager.getMethod(Scene.v().getMainMethod())
        );
    }

    @Override
    public boolean canAssign(Type from, Type to) {
        return hierarchy.canStoreType(
                ((JimpleType) from).getSootType(),
                ((JimpleType) to).getSootType());
    }

    @Override
    public Method resolveInterfaceOrVirtualCall(Type recvType, Method method) {
        JimpleType jType = (JimpleType) recvType;
        JimpleMethod jMethod = (JimpleMethod) method;
        SootMethod callee = hierarchy.resolveConcreteDispatch(
                jType.getSootClass(),
                jMethod.getSootMethod());
        return elementManager.getMethod(callee);
    }

    @Override
    public Method resolveSpecialCall(CallSite callSite, Method container) {
        JimpleCallSite jCallSite = (JimpleCallSite) callSite;
        JimpleMethod jContainer = (JimpleMethod) container;
        SootMethod callee = hierarchy.resolveSpecialDispatch(
                (SpecialInvokeExpr) jCallSite.getSootInvokeExpr(),
                jContainer.getSootMethod());
        return elementManager.getMethod(callee);
    }
}
