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

package pascal.taie.util;

import org.junit.Test;

import java.util.Set;

public class ArraySetTest extends AbstractSetTest {

    protected <E> Set<E> newSet() {
        return new ArraySet<>();
    }

    @Test(expected = TooManyElementsException.class)
    public void testFixedCapacity() {
        testAddNElements(new ArraySet<>(4), 5);
    }

    @Test
    public void testNonFixedCapacity() {
        testAddNElements(new ArraySet<>(4, false), 5);
    }
}
