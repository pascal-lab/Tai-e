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

package pascal.taie.language;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import static pascal.taie.language.type.NullType.NULL;
import static pascal.taie.language.type.PrimitiveType.INT;
import static pascal.taie.language.type.PrimitiveType.LONG;

public class TypeTest {

    private static TypeManager typeManager;

    @BeforeClass
    public static void initTypeManager() {
        Main.buildWorld("-cp", "src/test/resources/basic", "-m", "Types");
        typeManager = World.get().getTypeManager();
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

    @Test
    public void testGetType1() {
        Type a = typeManager.getClassType("A");
        Type aArray = typeManager.getArrayType(a, 1);
        Type aArray2 = typeManager.getType("A[]");
        Assert.assertEquals(aArray, aArray2);

        Type intArray = typeManager.getArrayType(INT, 2);
        Type intArray2 = typeManager.getType("int[][]");
        Assert.assertEquals(intArray, intArray2);
    }
}
