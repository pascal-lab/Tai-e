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

package pascal.taie.java;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.java.types.Type;
import pascal.taie.java.types.TypeManagerImpl;

public class TypeTest {

    private final TypeManager typeManager = new TypeManagerImpl(null);

    @Test
    public void testPrimitiveType() {
        Type intType = typeManager.getPrimitiveType("int");
        Assert.assertEquals("int", intType.getName());
    }
}
