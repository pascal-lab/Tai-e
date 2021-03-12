/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.newpta.core.context;

import org.junit.Assert;
import org.junit.Test;

public class ContextTest {

    @Test
    public void testLinkedContext() {
        TreeContext.Factory<String> factory = new TreeContext.Factory<>();
        Context a = factory.get("A");
        Context ab1 = factory.append(a, "B", 2);
        Context ab2 = factory.append(a, "B", 3);
        Assert.assertEquals(ab1, ab2);
        Context b1 = factory.get("B");
        Context b2 = factory.append(a, "B", 1);
        Assert.assertEquals(b1, b2);
        Context bc = factory.append(ab1, "C", 2);
        Context abc = factory.append(ab1, "C", 3);
        TreeContext<String> bcd = factory.append(abc, "D", 3);
        Assert.assertEquals(bc, bcd.getParent());
        TreeContext<String> cde = factory.append(bcd, "E", 3);
        Context cd = factory.get("C", "D");
        Assert.assertEquals(cde.getParent(), cd);
    }
}
