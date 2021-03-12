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

package pascal.taie.newpta.core.context;

import pascal.taie.newpta.core.cs.CSMethod;
import pascal.taie.newpta.core.heap.NewObj;
import pascal.taie.newpta.core.heap.Obj;

abstract class AbstractContextSelector<T> implements ContextSelector {

    protected final ContextFactory<T> factory = new TreeContext.Factory<>();

    @Override
    public Context getDefaultContext() {
        return factory.getDefaultContext();
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        // Uses different strategies to select heap contexts
        // for different kinds of objects.
        if (obj instanceof NewObj) {
            return doSelectHeapContext(method, obj);
        } else {
            return getDefaultContext();
        }
    }

    /**
     * This method defines the real heap context selector.
     */
    protected abstract Context doSelectHeapContext(CSMethod method, Obj obj);
}
