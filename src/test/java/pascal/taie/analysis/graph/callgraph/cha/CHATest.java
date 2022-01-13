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

package pascal.taie.analysis.graph.callgraph.cha;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class CHATest {
    
    protected static void test(String main) {
        Tests.test(main, "src/test/resources/cha/", "cg", "algorithm:cha");
    }

    @Test
    public void testStaticCall() {
        test("StaticCall");
    }

    @Test
    public void testVirtualCall() {
        test("VirtualCall");
    }

    @Test
    public void testInterface() {
        test("Interface");
    }

    @Test
    public void testAbstractMethod() {
        test("AbstractMethod");
    }
}
