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
import pascal.taie.util.collection.ArrayMapTest;
import pascal.taie.util.collection.ArraySetTest;
import pascal.taie.util.collection.CollectionUtilsTest;
import pascal.taie.util.collection.HybridArrayHashMapTest;
import pascal.taie.util.collection.HybridArrayHashSetTest;
import pascal.taie.util.collection.IndexMapTest;
import pascal.taie.util.collection.SetQueueTest;
import pascal.taie.util.graph.GraphTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // collection
        ArraySetTest.class,
        ArrayMapTest.class,
        HybridArrayHashMapTest.class,
        HybridArrayHashSetTest.class,
        IndexMapTest.class,
        SetQueueTest.class,
        CollectionUtilsTest.class,
        // graph
        GraphTest.class,
})
public class UtilTestSuite {
}
