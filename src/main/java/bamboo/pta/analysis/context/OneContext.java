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

package bamboo.pta.analysis.context;

import java.util.Collections;
import java.util.List;

/**
 * Context with one element
 * @param <T>
 */
class OneContext<T> implements Context {

    private final T element;

    OneContext(T element) {
        this.element = element;
    }

    @Override
    public List<T> getElements() {
        return Collections.singletonList(element);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OneContext<?> that = (OneContext<?>) o;
        return element.equals(that.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public String toString() {
        return "[" + element + "]";
    }
}
