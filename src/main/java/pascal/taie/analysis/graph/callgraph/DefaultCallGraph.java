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

import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MapUtils;

/**
 * Default implementation of call graph.
 */
public class DefaultCallGraph extends AbstractCallGraph<Invoke, JMethod> {

    @Override
    protected boolean addNewMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            if (!method.isAbstract()) {
                method.getIR().getStmts().forEach(stmt -> {
                    if (stmt instanceof Invoke) {
                        Invoke invoke = (Invoke) stmt;
                        callSiteToContainer.put(invoke, method);
                        MapUtils.addToMapSet(callSitesIn, method, invoke);
                    }
                });
            }
            return true;
        }
        return false;
    }
}
