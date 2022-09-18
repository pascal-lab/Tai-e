/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
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
        Main.buildWorld("-cp", "src/test/resources/world", "--input-classes", "Types");
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
