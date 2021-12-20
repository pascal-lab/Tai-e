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

import pascal.taie.util.Hashes;

import java.util.Objects;

public class Pair<T1, T2> {

    private final T1 first;
    private final T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public T1 getFirst() {
        return first;
    }

    public T2 getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pair) {
            Pair<?, ?> anoPair = (Pair<?, ?>) o;
            return Objects.equals(first, anoPair.first)
                    && Objects.equals(second, anoPair.second);
        }
        return false;
    }

    @Override
    public String toString() {
        return "<" + first + ", " + second + ">";
    }
}
