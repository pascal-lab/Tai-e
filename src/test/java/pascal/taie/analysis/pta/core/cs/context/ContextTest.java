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

package pascal.taie.analysis.pta.core.cs.context;

import org.junit.Assert;
import org.junit.Test;

public class ContextTest {

    @Test
    public void testLinkedContext() {
        TreeContext.Factory<String> factory = new TreeContext.Factory<>();
        Context a = factory.make("A");
        Context ab1 = factory.append(a, "B", 2);
        Context ab2 = factory.append(a, "B", 3);
        Assert.assertEquals(ab1, ab2);
        Context b1 = factory.make("B");
        Context b2 = factory.append(a, "B", 1);
        Assert.assertEquals(b1, b2);
        Context bc = factory.append(ab1, "C", 2);
        Context abc = factory.append(ab1, "C", 3);
        TreeContext<String> bcd = factory.append(abc, "D", 3);
        Assert.assertEquals(bc, bcd.getParent());
        TreeContext<String> cde = factory.append(bcd, "E", 3);
        Context cd = factory.make("C", "D");
        Assert.assertEquals(cde.getParent(), cd);
    }
}
