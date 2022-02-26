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
import pascal.taie.language.type.TypeSystem;

import static pascal.taie.language.type.NullType.NULL;
import static pascal.taie.language.type.PrimitiveType.INT;
import static pascal.taie.language.type.PrimitiveType.LONG;

public class TypeTest {

    private static TypeSystem typeSystem;

    @BeforeClass
    public static void initTypeManager() {
        Main.buildWorld("-cp", "src/test/resources/basic", "-m", "Types");
        typeSystem = World.get().getTypeSystem();
    }

    @Test
    public void testSubtypeNull() {
        Type object = typeSystem.getClassType("java.lang.Object");
        Type intArray = typeSystem.getArrayType(INT, 1);

        Assert.assertTrue(typeSystem.isSubtype(object, NULL));
        Assert.assertFalse(typeSystem.isSubtype(INT, NULL));
        Assert.assertTrue(typeSystem.isSubtype(intArray, NULL));
    }

    @Test
    public void testSubtypePrimitive() {
        Assert.assertTrue(typeSystem.isSubtype(INT, INT));
        Assert.assertFalse(typeSystem.isSubtype(INT, LONG));
    }

    /**
     * Test arrays and some special classes.
     */
    @Test
    public void testSubtypeArray1() {
        Type intArray = typeSystem.getArrayType(INT, 1);
        Type object = typeSystem.getClassType("java.lang.Object");
        Assert.assertTrue(typeSystem.isSubtype(object, intArray));

        Type serializable = typeSystem.getClassType("java.lang.Serializable");
        Assert.assertTrue(typeSystem.isSubtype(serializable, intArray));

        Type cloneable = typeSystem.getClassType("java.lang.Cloneable");
        Assert.assertTrue(typeSystem.isSubtype(cloneable, intArray));

        Type a = typeSystem.getClassType("A");
        Assert.assertFalse(typeSystem.isSubtype(a, intArray));
    }

    /**
     * Test arrays with different dimensions.
     */
    @Test
    public void testSubtypeArray2() {
        Type intArray2 = typeSystem.getArrayType(INT, 2);
        Type object = typeSystem.getClassType("java.lang.Object");
        Type objectArray = typeSystem.getArrayType(object, 1);
        Assert.assertTrue(typeSystem.isSubtype(objectArray, intArray2));

        Type intArray3 = typeSystem.getArrayType(INT, 3);
        Assert.assertTrue(typeSystem.isSubtype(objectArray, intArray3));
    }

    /**
     * Test arrays of different classes with subtyping relation.
     */
    @Test
    public void testSubtypeArray3() {
        Type object = typeSystem.getClassType("java.lang.Object");
        Type objectArray = typeSystem.getArrayType(object, 1);
        Type a = typeSystem.getClassType("A");
        Type aArray = typeSystem.getArrayType(a, 1);
        Assert.assertTrue(typeSystem.isSubtype(objectArray, aArray));

        Type b = typeSystem.getClassType("B");
        Type bArray = typeSystem.getArrayType(b, 1);
        Assert.assertTrue(typeSystem.isSubtype(aArray, bArray));
        Assert.assertFalse(typeSystem.isSubtype(bArray, aArray));
    }

    @Test
    public void testGetType1() {
        Type a = typeSystem.getClassType("A");
        Type aArray = typeSystem.getArrayType(a, 1);
        Type aArray2 = typeSystem.getType("A[]");
        Assert.assertEquals(aArray, aArray2);

        Type intArray = typeSystem.getArrayType(INT, 2);
        Type intArray2 = typeSystem.getType("int[][]");
        Assert.assertEquals(intArray, intArray2);
    }
}
