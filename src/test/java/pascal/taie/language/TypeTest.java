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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.NullType.NULL;

public class TypeTest {

    private static TypeSystem typeSystem;

    @BeforeAll
    public static void initTypeManager() {
        Main.buildWorld("-cp", "src/test/resources/world", "--input-classes", "Types");
        typeSystem = World.get().getTypeSystem();
    }

    @Test
    void testSubtypeNull() {
        Type object = typeSystem.getClassType(ClassNames.OBJECT);
        Type intArray = typeSystem.getArrayType(INT, 1);

        assertTrue(typeSystem.isSubtype(object, NULL));
        assertFalse(typeSystem.isSubtype(INT, NULL));
        assertTrue(typeSystem.isSubtype(intArray, NULL));
    }

    @Test
    void testSubtypePrimitive() {
        assertTrue(typeSystem.isSubtype(INT, INT));
        assertFalse(typeSystem.isSubtype(INT, LONG));
    }

    /**
     * Test arrays and some special classes.
     */
    @Test
    void testSubtypeArray1() {
        Type intArray = typeSystem.getArrayType(INT, 1);
        Type object = typeSystem.getClassType(ClassNames.OBJECT);
        assertTrue(typeSystem.isSubtype(object, intArray));

        Type serializable = typeSystem.getClassType(ClassNames.SERIALIZABLE);
        assertTrue(typeSystem.isSubtype(serializable, intArray));

        Type cloneable = typeSystem.getClassType(ClassNames.CLONEABLE);
        assertTrue(typeSystem.isSubtype(cloneable, intArray));

        Type a = typeSystem.getClassType("A");
        assertFalse(typeSystem.isSubtype(a, intArray));
    }

    /**
     * Test arrays with different dimensions.
     */
    @Test
    void testSubtypeArray2() {
        Type intArray2 = typeSystem.getArrayType(INT, 2);
        Type object = typeSystem.getClassType(ClassNames.OBJECT);
        Type objectArray = typeSystem.getArrayType(object, 1);
        assertTrue(typeSystem.isSubtype(objectArray, intArray2));

        Type intArray3 = typeSystem.getArrayType(INT, 3);
        assertTrue(typeSystem.isSubtype(objectArray, intArray3));
    }

    /**
     * Test arrays of different classes with subtyping relation.
     */
    @Test
    void testSubtypeArray3() {
        Type object = typeSystem.getClassType(ClassNames.OBJECT);
        Type objectArray = typeSystem.getArrayType(object, 1);
        Type a = typeSystem.getClassType("A");
        Type aArray = typeSystem.getArrayType(a, 1);
        assertTrue(typeSystem.isSubtype(objectArray, aArray));

        Type b = typeSystem.getClassType("B");
        Type bArray = typeSystem.getArrayType(b, 1);
        assertTrue(typeSystem.isSubtype(aArray, bArray));
        assertFalse(typeSystem.isSubtype(bArray, aArray));
    }

    @Test
    void testGetType1() {
        Type a = typeSystem.getClassType("A");
        Type aArray = typeSystem.getArrayType(a, 1);
        Type aArray2 = typeSystem.getType("A[]");
        assertEquals(aArray, aArray2);

        Type intArray = typeSystem.getArrayType(INT, 2);
        Type intArray2 = typeSystem.getType("int[][]");
        assertEquals(intArray, intArray2);
    }
}
