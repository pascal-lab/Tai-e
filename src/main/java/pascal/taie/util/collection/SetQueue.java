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

package pascal.taie.util.collection;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A Queue implementation which contains no duplicate elements.
 * @param <E> type of elements.
 */
public class SetQueue<E> extends AbstractQueue<E> {

    private final Set<E> set = new LinkedHashSet<>();

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean add(E e) {
        return set.add(e);
    }

    @Override
    public boolean offer(E e) {
        return set.add(e);
    }

    @Override
    public E poll() {
        Iterator<E> it = set.iterator();
        if (it.hasNext()) {
            E e = it.next();
            it.remove();
            return e;
        } else {
            return null;
        }
    }

    @Override
    public E peek() {
        Iterator<E> it = set.iterator();
        return it.hasNext() ? it.next() : null;
    }
}
