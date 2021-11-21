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

package pascal.taie.analysis.pta.core.cs.context;

import java.util.Arrays;
import java.util.Map;

import static pascal.taie.util.collection.Maps.newHybridMap;

/**
 * An implementation of {@link Context}, which organizes contexts like a tree.
 * Each context has a parent context and zero or more children contexts.
 * For example, for Context[A, B], its parent is Context@[A], and its children
 * may be Context@[A, B, C] or Context@[A, B, D].
 * {@link TreeContext.Factory} ensures that the contexts with the same elements
 * will be created at most once. Thus, we can avoid creating redundant
 * context objects, and test their equality by efficient ==.
 *
 * @param <T> type of context elements.
 */
public class TreeContext<T> implements Context {

    private final TreeContext<T> parent;

    private final T elem;

    private final int length;

    private Map<T, TreeContext<T>> children;

    private TreeContext() {
        parent = null;
        elem = null;
        length = 0;
    }

    private TreeContext(TreeContext<T> parent, T elem) {
        this.parent = parent;
        this.elem = elem;
        this.length = parent.getLength() + 1;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public T getElementAt(int i) {
        assert 0 < i && i <= length;
        if (i == length - 1) {
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
        Object[] elems = new Object[length];
        TreeContext<T> c = this;
        for (int i = length - 1; i >= 0; --i) {
            elems[i] = c.getElem();
            c = c.getParent();
        }
        return Arrays.toString(elems);
    }

    public static class Factory<T> implements ContextFactory<T> {

        /**
         * Root context of all tree contexts produced by this factory.
         * It also acts as the default context.
         */
        private final TreeContext<T> rootContext = new TreeContext<>();

        @Override
        public TreeContext<T> getEmptyContext() {
            return rootContext;
        }

        @Override
        public Context make(T elem) {
            return rootContext.getChild(elem);
        }

        @Override
        public TreeContext<T> make(T... elems) {
            TreeContext<T> result = rootContext;
            for (T elem : elems) {
                result = result.getChild(elem);
            }
            return result;
        }

        @Override
        public TreeContext<T> makeLastK(Context context, int k) {
            if (k == 0) {
                return rootContext;
            }
            TreeContext<T> c = (TreeContext<T>) context;
            if (c.getLength() <= k) {
                return c;
            }
            Object[] elems = new Object[k];
            for (int i = k; i > 0; --i) {
                elems[i - 1] = c.getElem();
                c = c.getParent();
            }
            return make((T[]) elems);
        }

        @Override
        public TreeContext<T> append(Context parent, T elem, int limit) {
            TreeContext<T> p = (TreeContext<T>) parent;
            if (parent.getLength() < limit) {
                return p.getChild(elem);
            } else {
                return makeLastK(p, limit - 1).getChild(elem);
            }
        }
    }
}
