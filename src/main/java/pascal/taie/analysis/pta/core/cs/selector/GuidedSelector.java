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
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import java.util.Map;
import java.util.Set;

/**
 * Guided context selector.
 */
@SuppressWarnings("ALL")
class GuidedSelector extends AbstractContextSelector {

    /**
     * Default context sensitivity variant for the methods that are not specified.
     */
    private static final String DEFAULT_CS = "ci";

    /**
     * Supported context sensitivity variants.
     */
    private static final Set<String> SUPPORTED_CS = Set.of(
            "ci", "1-type", "2-type", "2-obj"
    );

    /**
     * Default limit for heap contexts.
     */
    private static final int DEFAULT_HLIMIT = 1;

    /**
     * Guide for context sensitivity variant selection.
     */
    private final Map<JMethod, String> csMap;

    /**
     * Limit for heap contexts.
     */
    private final int hlimit;

    GuidedSelector(Map<JMethod, String> csMap, int hlimit) {
        this.csMap = csMap;
        this.hlimit = hlimit;
    }

    GuidedSelector(Map<JMethod, String> csMap) {
        this(csMap, DEFAULT_HLIMIT);
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        String cs = csMap.getOrDefault(callee, DEFAULT_CS);
        return switch (cs) {
            case "ci" -> selectCI();
            case "1-type" -> select1Type(recv);
            case "2-type" -> select2Type(recv);
            case "2-obj" -> select2Obj(recv);
            default -> throw new AnalysisException(cs + " is not supported " +
                    "(currently supported cs: " + SUPPORTED_CS + ")");
        };
    }

    private Context selectCI() {
        return factory.getEmptyContext();
    }

    private Context select1Type(CSObj recv) {
        return factory.make(recv.getObject().getContainerType());
    }

    private Context select2Type(CSObj recv) {
        Type ctxElem2 = recv.getObject().getContainerType();
        Context hctx = recv.getContext();
        if (hctx.getLength() == 0) {
            return factory.make(ctxElem2);
        } else {
            Object ctxElem1 = hctx.getElementAt(hctx.getLength() - 1);
            if (ctxElem1 instanceof Type) {
                return factory.make(ctxElem1, ctxElem2);
            } else if (ctxElem1 instanceof Obj) {
                return factory.make(((Obj) ctxElem1).getContainerType(), ctxElem2);
            } else {
                throw new AnalysisException("Unexpected context element: " + ctxElem1);
            }
        }
    }

    private Context select2Obj(CSObj recv) {
        Obj ctxElem2 = recv.getObject();
        Context hctx = recv.getContext();
        if (hctx.getLength() == 0) {
            return factory.make(ctxElem2);
        } else {
            Object ctxElem1 = hctx.getElementAt(hctx.getLength() - 1);
            return factory.make(ctxElem1, ctxElem2);
        }
    }

    @Override
    protected Context selectNewObjContext(CSMethod method, NewObj obj) {
        return factory.makeLastK(method.getContext(), hlimit);
    }
}
