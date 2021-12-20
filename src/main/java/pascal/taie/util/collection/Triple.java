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

public class Triple<T1, T2, T3> {

    private final T1 first;
    private final T2 second;
    private final T3 third;

    public Triple(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T1 getFirst() {
        return first;
    }

    public T2 getSecond() {
        return second;
    }

    public T3 getThird() {
        return third;
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(first, second, third);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Triple) {
            Triple<?, ?, ?> anoTriple = (Triple<?, ?, ?>) o;
            return Objects.equals(first, anoTriple.first)
                    && Objects.equals(second, anoTriple.second)
                    && Objects.equals(third, anoTriple.third);
        }
        return false;
    }

    @Override
    public String toString() {
        return "<" + first + ", " + second + ", " + third + ">";
    }
}
