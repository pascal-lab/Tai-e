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

import java.util.Map;

import static pascal.taie.util.CollectionUtils.newHybridMap;

class LinkedContext<T> implements Context {

    private final LinkedContext<T> parent;

    private final T elem;

    private final int depth;

    private Map<T, LinkedContext<T>> children;

    LinkedContext() {
        parent = null;
        elem = null;
        depth = 0;
    }

    private LinkedContext(LinkedContext<T> parent, T elem) {
        this.parent = parent;
        this.elem = elem;
        this.depth = parent.getDepth() + 1;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public T getElementAt(int i) {
        assert 0 < i && i <= depth;
        if (i == depth) {
            return elem;
        } else {
            return parent.getElementAt(i);
        }
    }

    LinkedContext<T> getParent() {
        return parent;
    }

    LinkedContext<T> getChild(T elem) {
        if (children == null) {
            children = newHybridMap();
        }
        return children.computeIfAbsent(elem,
                e -> new LinkedContext<>(this, e));
    }

    T getElem() {
        return elem;
    }
}
