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

import java.util.Arrays;
import java.util.Map;

import static pascal.taie.util.CollectionUtils.newHybridMap;

/**
 * Contexts that are organized like a tree.
 * Each context has a parent context and zero or more children contexts.
 * For example, for Context[A, B], its parent is Context@[A], and its children
 * may be Context@[A, B, C] or Context@[A, B, D].
 * {@link TreeContext.Factory} ensures that the contexts with the same elements
 * have only one instance. Thus, we can avoid creating redundant
 * context objects, and test their equality by efficient ==.
 *
 * @param <T> type of context elements.
 */
class TreeContext<T> implements Context {

    private final TreeContext<T> parent;

    private final T elem;

    private final int depth;

    private Map<T, TreeContext<T>> children;

    private TreeContext() {
        parent = null;
        elem = null;
        depth = 0;
    }

    private TreeContext(TreeContext<T> parent, T elem) {
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

    TreeContext<T> getParent() {
        return parent;
    }

    TreeContext<T> getChild(T elem) {
        if (children == null) {
            children = newHybridMap();
        }
        return children.computeIfAbsent(elem,
                e -> new TreeContext<>(this, e));
    }

    T getElem() {
        return elem;
    }

    @Override
    public String toString() {
        Object[] elems = new Object[depth];
        TreeContext<T> c = this;
        for (int i = depth - 1; i >= 0; --i) {
            elems[i] = c.getElem();
            c = c.getParent();
        }
        return Arrays.toString(elems);
    }

    static class Factory<T> implements ContextFactory<T> {

        /**
         * Root context of all tree contexts produced by this factory.
         * It also acts as the default context.
         */
        private final TreeContext<T> rootContext = new TreeContext<>();

        @Override
        public TreeContext<T> getDefaultContext() {
            return rootContext;
        }

        @Override
        public Context get(T elem) {
            return rootContext.getChild(elem);
        }

        @Override
        public TreeContext<T> get(T... elems) {
            TreeContext<T> result = rootContext;
            for (T elem : elems) {
                result = result.getChild(elem);
            }
            return result;
        }

        @Override
        public TreeContext<T> getLastK(Context context, int k) {
            if (k == 0) {
                return rootContext;
            }
            TreeContext<T> c = (TreeContext<T>) context;
            if (c.getDepth() <= k) {
                return c;
            }
            Object[] elems = new Object[k];
            for (int i = k; i > 0; --i) {
                elems[i - 1] = c.getElem();
                c = c.getParent();
            }
            return get((T[]) elems);
        }

        @Override
        public TreeContext<T> append(Context parent, T elem, int limit) {
            TreeContext<T> p = (TreeContext<T>) parent;
            if (parent.getDepth() < limit) {
                return p.getChild(elem);
            } else {
                return getLastK(p, limit - 1).getChild(elem);
            }
        }
    }
}
