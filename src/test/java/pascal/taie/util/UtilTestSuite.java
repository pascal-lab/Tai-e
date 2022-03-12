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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.taie.util.collection.AbstractBitSetTest;
import pascal.taie.util.collection.ArrayMapTest;
import pascal.taie.util.collection.ArraySetTest;
import pascal.taie.util.collection.BitSetTest;
import pascal.taie.util.collection.HybridArrayHashMapTest;
import pascal.taie.util.collection.HybridArrayHashSetTest;
import pascal.taie.util.collection.MultiMapTest;
import pascal.taie.util.collection.SetQueueTest;
import pascal.taie.util.collection.StreamsTest;
import pascal.taie.util.collection.TwoKeyMapTest;
import pascal.taie.util.collection.ViewsTest;
import pascal.taie.util.graph.GraphTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // collection
        AbstractBitSetTest.class,
        ArraySetTest.class,
        ArrayMapTest.class,
        BitSetTest.class,
        HybridArrayHashMapTest.class,
        HybridArrayHashSetTest.class,
        MultiMapTest.class,
        TwoKeyMapTest.class,
        SetQueueTest.class,
        StreamsTest.class,
        ViewsTest.class,
        // graph
        GraphTest.class,
        // others
        MapperTest.class,
})
public class UtilTestSuite {
}
