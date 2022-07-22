/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.core.cs.context;

import pascal.taie.util.collection.Maps;

import java.util.Arrays;
import java.util.Map;

/**
 * An implementation of {@link Context}, which organizes contexts as Trie.
 */
public class TrieContext implements Context {

    private final TrieContext parent;

    private final Object elem;

    private final int length;

    private Map<Object, TrieContext> children;

    private TrieContext() {
        parent = null;
        elem = null;
        length = 0;
    }

    private TrieContext(TrieContext parent, Object elem) {
        this.parent = parent;
        this.elem = elem;
        this.length = parent.getLength() + 1;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public Object getElementAt(int i) {
        assert 0 <= i && i < length;
        if (i == length - 1) {
            return elem;
        } else {
            return parent.getElementAt(i);
        }
    }

    TrieContext getParent() {
        return parent;
    }

    TrieContext getChild(Object elem) {
        if (children == null) {
            children = Maps.newHybridMap();
        }
        return children.computeIfAbsent(elem,
                e -> new TrieContext(this, e));
    }

    Object getElem() {
        return elem;
    }

    @Override
    public String toString() {
        Object[] elems = new Object[length];
        TrieContext c = this;
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
        private final TrieContext rootContext = new TrieContext();

        @Override
        public TrieContext getEmptyContext() {
            return rootContext;
        }

        @Override
        public Context make(T elem) {
            return rootContext.getChild(elem);
        }

        @Override
        public TrieContext make(T... elems) {
            TrieContext result = rootContext;
            for (T elem : elems) {
                result = result.getChild(elem);
            }
            return result;
        }

        @Override
        public TrieContext makeLastK(Context context, int k) {
            if (k == 0) {
                return rootContext;
            }
            TrieContext c = (TrieContext) context;
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
        public TrieContext append(Context parent, T elem, int limit) {
            TrieContext p = (TrieContext) parent;
            if (parent.getLength() < limit) {
                return p.getChild(elem);
            } else {
                return makeLastK(p, limit - 1).getChild(elem);
            }
        }
    }
}
