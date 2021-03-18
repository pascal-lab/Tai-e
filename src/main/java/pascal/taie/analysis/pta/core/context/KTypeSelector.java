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

package pascal.taie.analysis.pta.core.context;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;
import pascal.taie.analysis.pta.core.cs.CSCallSite;
import pascal.taie.analysis.pta.core.cs.CSObj;

public class KTypeSelector extends KContextSelector<Type> {

    public KTypeSelector(int k, int hk) {
        super(k, hk);
    }

    public KTypeSelector(int k) {
        this(k, k - 1);
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        return factory.append(
                recv.getContext(), recv.getObject().getContainerType(), limit);
    }
}
