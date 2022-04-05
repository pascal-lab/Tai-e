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

public class SparseBitSetTest extends BitSetTest {

    @Override
    protected BitSet of(int... indexes) {
        BitSet result = new SparseBitSet();
        for (int i : indexes) {
            result.set(i);
        }
        return result;
    }
}
