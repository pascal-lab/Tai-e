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

package pascal.taie.analysis.oldpta.core.context;

import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.oldpta.core.cs.CSCallSite;
import pascal.taie.analysis.oldpta.core.cs.CSMethod;
import pascal.taie.analysis.oldpta.core.cs.CSObj;
import pascal.taie.analysis.oldpta.ir.Obj;

public interface ContextSelector {

    default Context getDefaultContext() {
        return DefaultContext.INSTANCE;
    }

    /**
     * Selects contexts for static methods.
     */
    Context selectContext(CSCallSite callSite, JMethod callee);

    /**
     * Selects contexts for instance methods.
     */
    Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee);

    /**
     * Selects heap contexts for new-created abstract objects.
     */
    Context selectHeapContext(CSMethod method, Obj obj);
}
