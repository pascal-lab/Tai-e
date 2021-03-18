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

package pascal.taie.language;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.frontend.soot.TestUtils;
import pascal.taie.language.types.Type;

import java.io.File;

import static pascal.taie.language.types.NullType.NULL;
import static pascal.taie.language.types.PrimitiveType.INT;
import static pascal.taie.language.types.PrimitiveType.LONG;

public class TypeTest {

    private static TypeManager typeManager;

    @BeforeClass
    public static void initTypeManager() {
        TestUtils.buildWorld(new String[]{
                "-cp",
                String.join(File.pathSeparator,
                        "java-benchmarks/jre1.6.0_24/rt.jar",
                        "java-benchmarks/jre1.6.0_24/jce.jar",
                        "java-benchmarks/jre1.6.0_24/jsse.jar",
                        "test-resources/java"),
                "Types"
        });
        typeManager = World.getTypeManager();
    }

    @Test
    public void testSubtypeNull() {
        Type object = typeManager.getClassType("java.lang.Object");
        Type intArray = typeManager.getArrayType(INT, 1);

        Assert.assertTrue(typeManager.isSubtype(object, NULL));
        Assert.assertFalse(typeManager.isSubtype(INT, NULL));
        Assert.assertTrue(typeManager.isSubtype(intArray, NULL));
    }

    @Test
    public void testSubtypePrimitive() {
        Assert.assertTrue(typeManager.isSubtype(INT, INT));
        Assert.assertFalse(typeManager.isSubtype(INT, LONG));
    }

    /**
     * Test arrays and some special classes.
     */
    @Test
    public void testSubtypeArray1() {
        Type intArray = typeManager.getArrayType(INT, 1);
        Type object = typeManager.getClassType("java.lang.Object");
        Assert.assertTrue(typeManager.isSubtype(object, intArray));

        Type serializable = typeManager.getClassType("java.lang.Serializable");
        Assert.assertTrue(typeManager.isSubtype(serializable, intArray));

        Type cloneable = typeManager.getClassType("java.lang.Cloneable");
        Assert.assertTrue(typeManager.isSubtype(cloneable, intArray));

        Type a = typeManager.getClassType("A");
        Assert.assertFalse(typeManager.isSubtype(a, intArray));
    }

    /**
     * Test arrays with different dimensions.
     */
    @Test
    public void testSubtypeArray2() {
        Type intArray2 = typeManager.getArrayType(INT, 2);
        Type object = typeManager.getClassType("java.lang.Object");
        Type objectArray = typeManager.getArrayType(object, 1);
        Assert.assertTrue(typeManager.isSubtype(objectArray, intArray2));

        Type intArray3 = typeManager.getArrayType(INT, 3);
        Assert.assertTrue(typeManager.isSubtype(objectArray, intArray3));
    }

    /**
     * Test arrays of different classes with subtyping relation.
     */
    @Test
    public void testSubtypeArray3() {
        Type object = typeManager.getClassType("java.lang.Object");
        Type objectArray = typeManager.getArrayType(object, 1);
        Type a = typeManager.getClassType("A");
        Type aArray = typeManager.getArrayType(a, 1);
        Assert.assertTrue(typeManager.isSubtype(objectArray, aArray));

        Type b = typeManager.getClassType("B");
        Type bArray = typeManager.getArrayType(b, 1);
        Assert.assertTrue(typeManager.isSubtype(aArray, bArray));
        Assert.assertFalse(typeManager.isSubtype(bArray, aArray));
    }
}
