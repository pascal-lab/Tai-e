/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

public class InvokedynamicPlugin implements Plugin {

    private PointerAnalysis pta;

    // Lambdas can be processed in LambdasPlugin
    private final boolean processLambdas = false;

    private Var indyResult;

    @Override
    public void initialize() {

    }

    @Override
    public void handleNewMethod(JMethod method) {
        System.out.println("hello handleNewMethod");
        extractInvokeDynamics(method.getIR()).forEach(invoke -> {
            indyResult = invoke.getResult();
            InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
            String bsmName = indy.getBootstrapMethodRef().getName();
            if (processLambdas
                    || ( !processLambdas && !"metafactory".equals(bsmName) && !"altMetafactory".equals(bsmName))){

            }
            JClass indyLookupClass = indy.getCallSite().getMethod().getDeclaringClass();
//            indyLookupClass.getDeclaredMethod();
//            indyLookupClass.getDeclaredMethod();

        });
    }

    private static Stream<Invoke> extractInvokeDynamics(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(s -> s.getInvokeExp() instanceof InvokeDynamic);
    }

}
