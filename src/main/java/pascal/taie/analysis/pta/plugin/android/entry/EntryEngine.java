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


package pascal.taie.analysis.pta.plugin.android.entry;

import pascal.taie.analysis.pta.plugin.android.configs.LIFECYCLE;
import pascal.taie.analysis.pta.plugin.android.utils.SootUtils;
import pascal.taie.util.collection.Sets;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EntryEngine {

    private static final String dummyClassName = "dummyMainClass";

    private Set<SootMethod> componentEntries;

    public EntryEngine() {
        componentEntries = Sets.newConcurrentSet();
    }

    public  void generateAllComponentLifecycle(Set<SootClass> componentsSets) {
        for (SootClass sc : componentsSets) {
            SootClass fSuperClz = SootUtils.queryComponentType(sc);
            if (fSuperClz != sc) {
                pickComponent(sc, fSuperClz.getName());
            }
        }
    }

    private  void pickComponent(SootClass sc, String type) {
        List<SootMethod> methodsToCall = new ArrayList<SootMethod>();
        SootClass superClass = Scene.v().getSootClassUnsafe(type);
        switch (type) {
            case "android.app.Activity":
                for (LIFECYCLE.ACTIVITY lifecycle : LIFECYCLE.ACTIVITY.values()) {
                    SootMethod m = superClass.getMethodUnsafe(lifecycle.getValue());
                    if (m == null) {
                        continue;
                    }
                    methodsToCall.add(m);
                }
                break;
            case "android.app.Service":
                for (LIFECYCLE.SERVICE lifecycle : LIFECYCLE.SERVICE.values()) {
                    SootMethod m = superClass.getMethodUnsafe(lifecycle.getValue());
                    if (m == null) {
                        continue;
                    }
                    methodsToCall.add(m);
                }
                break;
            case "android.content.BroadcastReceiver":
                for (LIFECYCLE.RECEIVE lifecycle : LIFECYCLE.RECEIVE.values()) {
                    SootMethod m = superClass.getMethodUnsafe(lifecycle.getValue());
                    if (m == null) {
                        continue;
                    }
                    methodsToCall.add(m);
                }
                break;
            case "android.content.ContentProvider":
                for (LIFECYCLE.PROVIDER lifecycle : LIFECYCLE.PROVIDER.values()) {
                    SootMethod m = superClass.getMethodUnsafe(lifecycle.getValue());
                    if (m == null) {
                        continue;
                    }
                    methodsToCall.add(m);
                }
                break;
            default:
                break;
        }
        if (!methodsToCall.isEmpty()) {
            SootMethod dummyMethod = this.createDummyInternal(methodsToCall, sc, type);
            componentEntries.add(dummyMethod);
        }
    }

    private SootMethod createDummyInternal(List<SootMethod> callMethodList, SootClass compClz, String compType) {
        SootClass dummyClass = Scene.v().getSootClass(dummyClassName);
        String methodName = dummyClass + "_" + compClz.getName().replace(".", "_").replace("$", "_");
        SootMethod entryMethod = new SootMethod(methodName, List.of(), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        dummyClass.addMethod(entryMethod);

        // addClass() declares the given class as a library class. We need to
        // fix this.
        entryMethod.getDeclaringClass().setApplicationClass();

        Body body = Jimple.v().newBody(entryMethod);
        entryMethod.setActiveBody(body);
        UnitPatchingChain units = body.getUnits();
        Local instant = Jimple.v().newLocal("r0", compClz.getType());
        body.getLocals().add(instant);

        NewExpr newExpr = Jimple.v().newNewExpr(compClz.getType());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(instant, newExpr);
        units.add(assignStmt);
        List<SootMethod> realMethodSet = new ArrayList<SootMethod>();
        for (SootMethod m : compClz.getMethods()) {
            if (m.isConstructor()) {
                realMethodSet.add(m);
            }
        }
        realMethodSet.addAll(callMethodList);
        for (SootMethod targetMethod : realMethodSet) {
            //return value
            JimpleLocal ret = null;
            if (!(targetMethod.getReturnType() instanceof VoidType)) {
                int index = body.getLocalCount();
                Local localRet = Jimple.v().newLocal("v" + index, targetMethod.getReturnType());
                body.getLocals().add(localRet);
                ret = (JimpleLocal) localRet;
            }
            //arguments initialize
            List<Value> args = new ArrayList<>();
            for (int i = 0; i < targetMethod.getParameterCount(); i++) {
                Type argType = targetMethod.getParameterType(i);
                int index = body.getLocalCount();
                Local localArg = Jimple.v().newLocal("v" + index, argType);
                body.getLocals().add(localArg);
                args.add(localArg);
                if (argType instanceof PrimType) {
                    switch (((PrimType) argType).getTypeAsString()) {
                        case "soot.FloatType" -> {
                            AssignStmt floatAssignStmt = Jimple.v().newAssignStmt(localArg, FloatConstant.v(3f));
                            units.add(floatAssignStmt);
                        }
                        case "soot.DoubleType" -> {
                            AssignStmt doubleAssignStmt = Jimple.v().newAssignStmt(localArg, DoubleConstant.v(4.0));
                            units.add(doubleAssignStmt);
                        }
                        default -> {
                            AssignStmt intAssignStmt = Jimple.v().newAssignStmt(localArg, IntConstant.v(5));
                            units.add(intAssignStmt);
                        }
                    }
                } else {
                    if (argType instanceof ArrayType) {
                        NewArrayExpr argNewExpr = Jimple.v().newNewArrayExpr(((ArrayType) argType).baseType, IntConstant.v(2));
                        AssignStmt argAssignStmt = Jimple.v().newAssignStmt(localArg, argNewExpr);
                        units.add(argAssignStmt);
                    } else if (argType instanceof RefType) {
                        NewExpr argNewExpr = Jimple.v().newNewExpr((RefType) argType);
                        AssignStmt argAssignStmt = Jimple.v().newAssignStmt(localArg, argNewExpr);
                        units.add(argAssignStmt);
                    }
                }
            }
            //create stmt
            InvokeExpr invokeExpr = null;
            if (targetMethod.isStatic()) {
                invokeExpr = Jimple.v().newStaticInvokeExpr(targetMethod.makeRef(), args);

            } else {
                try {
                    invokeExpr = Jimple.v().newVirtualInvokeExpr(instant, targetMethod.makeRef(), args);
                } catch (Exception exception) {
                    invokeExpr = Jimple.v().newInterfaceInvokeExpr(instant, targetMethod.makeRef(), args);
                }
            }
            Stmt stmt = null;
            if (ret != null) {
                stmt = Jimple.v().newAssignStmt(ret, invokeExpr);
            } else {
                stmt = Jimple.v().newInvokeStmt(invokeExpr);
            }
            units.add(stmt);
        }
        units.add(Jimple.v().newReturnVoidStmt());
        return entryMethod;

    }

    public void addComponentEntry(SootMethod sootMethod) {
        componentEntries.add(sootMethod);
    }

    public void addComponentEntries(Set<SootMethod> sootMethod) {
        componentEntries.addAll(sootMethod);
    }

    public void removeComponentEntry(SootMethod sootMethod) {
        componentEntries.remove(sootMethod);
    }

    public Set<SootMethod> getAllEntries() {
        return Collections.unmodifiableSet(componentEntries);
    }
}
