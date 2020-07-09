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

package bamboo.pta.core.context;

import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.element.Obj;
import bamboo.util.AnalysisException;

/**
 * All context selectors should inherit this class, and we can define
 * some uniform behaviors of context selectors here.
 */
abstract class AbstractContextSelector implements ContextSelector {

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        // Uses different strategies to select heap contexts
        // for different kinds of objects.
        switch (obj.getKind()) {
            case NORMAL:
                return doSelectHeapContext(method, obj);
            case STRING_CONSTANT:
            case CLASS:
            case METHOD:
            case FIELD:
            case CONSTRUCTOR:
                return getDefaultContext();
            default:
                throw new AnalysisException("Unhandled case: " + obj);
        }
    }

    /**
     * This method defines the real heap context selector.
     */
    protected abstract Context doSelectHeapContext(CSMethod method, Obj obj);
}
