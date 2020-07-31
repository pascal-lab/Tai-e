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

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NopStmt;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StmtSwitch;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.options.Options;

class RelevantUnitSwitch implements StmtSwitch {

    private boolean relevant = true;

    public void caseAssignStmt(AssignStmt stmt) {
        relevant = true;
        if (Options.v().allow_phantom_refs()) {
            Value right = stmt.getRightOp();
            if (right instanceof InvokeExpr) {
                relevant = isNotPhantom((InvokeExpr) right);
            }
        } else {
            relevant = true;
        }
    }

    public void caseBreakpointStmt(BreakpointStmt stmt) {
        relevant = false;
    }

    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        relevant = false;
    }

    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        relevant = false;
    }

    public void caseGotoStmt(GotoStmt stmt) {
        relevant = false;
    }

    public void caseIdentityStmt(IdentityStmt stmt) {
        relevant = true;
    }

    public void caseIfStmt(IfStmt stmt) {
        relevant = false;
    }

    public void caseInvokeStmt(InvokeStmt stmt) {
        if (soot.options.Options.v().allow_phantom_refs()) {
            relevant = isNotPhantom(stmt.getInvokeExpr());
        } else {
            relevant = true;
        }
    }

    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        relevant = false;
    }

    public void caseNopStmt(NopStmt stmt) {
        relevant = false;
    }

    public void caseRetStmt(RetStmt stmt) {
        relevant = false;
    }

    public void caseReturnStmt(ReturnStmt stmt) {
        relevant = true;
    }

    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
        relevant = false;
    }

    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        relevant = false;
    }

    public void caseThrowStmt(ThrowStmt stmt) {
        relevant = true;
    }

    public void defaultCase(Object obj) {
        throw new RuntimeException("uh, why is this invoked?");
    }

    boolean isRelevant() {
        return relevant;
    }

    private boolean isNotPhantom(InvokeExpr invoke) {
        return !invoke.getMethodRef().getDeclaringClass().isPhantom();
    }
}
