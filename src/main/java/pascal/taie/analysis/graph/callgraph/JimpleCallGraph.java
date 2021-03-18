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

import pascal.taie.util.CollectionUtils;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class JimpleCallGraph extends AbstractCallGraph<Unit, SootMethod> {

    /**
     * Adds a new method to this call graph.
     * Returns true if the method was not in this call graph.
     */
    @Override
    protected boolean addNewMethod(SootMethod method) {
        if (reachableMethods.add(method)) {
            if (method.isConcrete()) {
                for (Unit unit : method.retrieveActiveBody().getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        callSiteToContainer.put(stmt, method);
                        CollectionUtils.addToMapSet(callSitesIn, method, stmt);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
