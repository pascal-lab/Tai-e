/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.oldpta.core.context;

import pascal.taie.oldpta.core.cs.CSMethod;
import pascal.taie.oldpta.ir.Obj;
import pascal.taie.util.AnalysisException;

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
            case MERGED:
            case ENV:
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
