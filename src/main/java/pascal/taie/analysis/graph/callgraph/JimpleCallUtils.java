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

package pascal.taie.analysis.graph.callgraph;

import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;

public class JimpleCallUtils {

    public static CallKind getCallKind(Unit callSite) {
        InvokeExpr invoke = ((Stmt) callSite).getInvokeExpr();
        return JimpleCallUtils.getCallKind(invoke);
    }

    public static CallKind getCallKind(InvokeExpr invoke) {
        if (invoke instanceof InterfaceInvokeExpr) {
            return CallKind.INTERFACE;
        } else if (invoke instanceof VirtualInvokeExpr) {
            return CallKind.VIRTUAL;
        } else if (invoke instanceof SpecialInvokeExpr) {
            return CallKind.SPECIAL;
        } else if (invoke instanceof StaticInvokeExpr) {
            return CallKind.STATIC;
        } else {
            return CallKind.OTHER;
        }
    }
}
