/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.context;

import java.util.Objects;

/**
 * Contexts with two elements
 * @param <T> type of context elements
 */
public class TwoContext<T> implements Context {

    private final T e1;

    private final T e2;

    TwoContext(T e1, T e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
    public int depth() {
        return 2;
    }

    @Override
    public T element(int i) {
        switch (i) {
            case 1: return e1;
            case 2: return e2;
            default: throw new IllegalArgumentException(
                    "Context " + this + " doesn't have " + i + "-th element");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TwoContext<?> that = (TwoContext<?>) o;
        return e1.equals(that.e1) && e2.equals(that.e2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(e1, e2);
    }

    @Override
    public String toString() {
        return "[" + e1 + "," + e2 + "]";
    }
}
