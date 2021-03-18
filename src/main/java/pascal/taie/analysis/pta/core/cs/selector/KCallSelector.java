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

package pascal.taie.analysis.pta.core.cs.selector;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.language.classes.JMethod;

public class KCallSelector extends KContextSelector<InvokeExp> {

    public KCallSelector(int k, int hk) {
        super(k, hk);
    }

    public KCallSelector(int k) {
        this(k, k - 1);
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return factory.append(
                callSite.getContext(), callSite.getCallSite(), limit);
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        Context parent = callSite.getContext();
        return factory.append(parent, callSite.getCallSite(), limit);
    }
}
