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
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.frontend.soot.TestUtils;
import pascal.taie.java.types.Type;

public class TypeTest {

    private static TypeManager typeManager;

    @BeforeClass
    public static void initTypeManager() {
        TestUtils.buildWorld(new String[]{
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "analyzed/java",
                "Types"
        });
        typeManager = World.get().getTypeManager();
    }

    @Test
    public void testCanAssignNull() {
        Type nullType = typeManager.getNullType();
        Type object = typeManager.getClassType("java.lang.Object");
        Type intType = typeManager.getIntType();
        Type intArray = typeManager.getArrayType(intType, 1);

        Assert.assertTrue(typeManager.canAssign(object, nullType));
        Assert.assertFalse(typeManager.canAssign(intType, nullType));
        Assert.assertTrue(typeManager.canAssign(intArray, nullType));
    }

    @Test
    public void testCanAssignPrimitive() {
        Type intType = typeManager.getIntType();
        Type longType = typeManager.getLongType();
        Assert.assertTrue(typeManager.canAssign(intType, intType));
        Assert.assertFalse(typeManager.canAssign(intType, longType));
    }

    @Test
    public void testCanAssignArray1() {
        Type intType = typeManager.getIntType();
        Type intArray = typeManager.getArrayType(intType, 1);
        Type object = typeManager.getClassType("java.lang.Object");
        Assert.assertTrue(typeManager.canAssign(object, intArray));

        Type serializable = typeManager.getClassType("java.lang.Serializable");
        Assert.assertTrue(typeManager.canAssign(serializable, intArray));

        Type cloneable = typeManager.getClassType("java.lang.Cloneable");
        Assert.assertTrue(typeManager.canAssign(cloneable, intArray));

        Type a = typeManager.getClassType("A");
        Assert.assertFalse(typeManager.canAssign(a, intArray));
    }

    @Test
    public void testCanAssignArray2() {
        Type intType = typeManager.getIntType();
        Type intArray2 = typeManager.getArrayType(intType, 2);
        Type object = typeManager.getClassType("java.lang.Object");
        Type objectArray = typeManager.getArrayType(object, 1);
        Assert.assertTrue(typeManager.canAssign(objectArray, intArray2));

        Type intArray3 = typeManager.getArrayType(intType, 3);
        Assert.assertTrue(typeManager.canAssign(objectArray, intArray3));
    }

    @Test
    public void testCanAssignArray3() {
        Type object = typeManager.getClassType("java.lang.Object");
        Type objectArray = typeManager.getArrayType(object, 1);
        Type a = typeManager.getClassType("A");
        Type aArray = typeManager.getArrayType(a, 1);
        Assert.assertTrue(typeManager.canAssign(objectArray, aArray));

        Type b = typeManager.getClassType("B");
        Type bArray = typeManager.getArrayType(b, 1);
        Assert.assertTrue(typeManager.canAssign(aArray, bArray));
        Assert.assertFalse(typeManager.canAssign(bArray, aArray));
    }
}
