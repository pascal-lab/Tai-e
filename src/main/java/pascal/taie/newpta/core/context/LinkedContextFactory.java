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

public class LinkedContextFactory<T> implements ContextFactory<T> {

    private final LinkedContext<T> defaultContext = new LinkedContext<>();

    @Override
    public Context getDefaultContext() {
        return defaultContext;
    }

    @Override
    public Context get(T elem) {
        return defaultContext.getChild(elem);
    }

    @Override
    public Context append(Context parent, T elem, int limit) {
        LinkedContext<T> p = (LinkedContext<T>) parent;
        if (parent.getDepth() < limit) {
            return p.getChild(elem);
        } else {
            return findParent(p, limit - 1).getChild(elem);
        }
    }

    private LinkedContext<T> findParent(LinkedContext<T> c, int distance) {
        Object[] elems = new Object[distance];
        for (int i = distance; i > 0; --i) {
            elems[i - 1] = c.getElem();
            c = c.getParent();
        }
        LinkedContext<T> parent = defaultContext;
        for (int i = 0; i < distance; ++i) {
            parent = parent.getChild((T) elems[i]);
        }
        return parent;
    }
}
