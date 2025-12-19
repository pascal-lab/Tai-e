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

package pascal.taie.frontend.java.generics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.generics.ArrayTypeGSignature;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.generics.ClassTypeGSignature;
import pascal.taie.language.generics.MethodGSignature;
import pascal.taie.language.generics.ReferenceTypeGSignature;
import pascal.taie.language.generics.TypeGSignature;
import pascal.taie.language.generics.TypeParameter;
import pascal.taie.language.generics.TypeVariableGSignature;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for generics support in Tai-e frontend.
 * These tests verify that generic type information is correctly
 * preserved through the compilation pipeline from Java source to Tai-e IR.
 */
public class GenericsTest {

    private static final String CP = "src/test/resources/frontend/generics";

    @BeforeEach
    void setUp() {
        World.reset();
    }

    private void buildWorld(String... inputClasses) {
        Main.buildWorld("-pp", "-cp", CP, "--input-classes",
                String.join(",", inputClasses));
    }

    private JClass getClass(String name) {
        return World.get().getClassHierarchy().getClass(name);
    }

    @Test
    void testGenericClass() {
        buildWorld("GenericClass");
        JClass clz = getClass("GenericClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig, "Generic signature should not be null");

        // class: GenericClass<T>
        List<TypeParameter> typeParams = gSig.getTypeParams();
        assertEquals(1, typeParams.size());
        assertEquals("T", typeParams.get(0).getTypeName());
    }

    @Test
    void testBoundedGenericClass() {
        buildWorld("BoundedGenericClass");
        JClass clz = getClass("BoundedGenericClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: BoundedGenericClass<T extends Number>
        List<TypeParameter> typeParams = gSig.getTypeParams();
        assertEquals(1, typeParams.size());
        TypeParameter tp = typeParams.get(0);
        assertEquals("T", tp.getTypeName());
        assertInstanceOf(ClassTypeGSignature.class, tp.getClassBound());
        assertEquals("java.lang.Number", tp.getClassBound().toString());
    }

    @Test
    void testMultiParamClass() {
        buildWorld("MultiParamClass");
        JClass clz = getClass("MultiParamClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: MultiParamClass<K, V>
        List<TypeParameter> typeParams = gSig.getTypeParams();
        assertEquals(2, typeParams.size());
        assertEquals("K", typeParams.get(0).getTypeName());
        assertEquals("V", typeParams.get(1).getTypeName());
    }

    @Test
    void testInterfaceBoundClass() {
        buildWorld("InterfaceBoundClass");
        JClass clz = getClass("InterfaceBoundClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: InterfaceBoundClass<T extends Comparable<T>>
        List<TypeParameter> typeParams = gSig.getTypeParams();
        assertEquals(1, typeParams.size());
        TypeParameter tp = typeParams.get(0);
        assertEquals("T", tp.getTypeName());
        // When a type parameter has only interface bounds (no class bound),
        // the class bound is either null or implicitly java.lang.Object
        // according to JVM specification (JVMS §4.7.9.1)
        ReferenceTypeGSignature classBound = tp.getClassBound();
        assertTrue(classBound == null || classBound.isJavaLangObject(),
                "Interface-only bound should have null or Object as class bound");
        assertEquals(1, tp.getInterfaceBounds().size());
        assertInstanceOf(ClassTypeGSignature.class, tp.getInterfaceBounds().get(0));
        assertEquals("java.lang.Comparable<T>", tp.getInterfaceBounds().get(0).toString());
    }

    @Test
    void testMultiBoundClass() {
        buildWorld("MultiBoundClass");
        JClass clz = getClass("MultiBoundClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: MultiBoundClass<T extends Number & Comparable<T> & Serializable>
        List<TypeParameter> typeParams = gSig.getTypeParams();
        assertEquals(1, typeParams.size());
        TypeParameter tp = typeParams.get(0);
        assertEquals("T", tp.getTypeName());
        assertInstanceOf(ClassTypeGSignature.class, tp.getClassBound());
        assertEquals("java.lang.Number", tp.getClassBound().toString());
        assertEquals(2, tp.getInterfaceBounds().size());
        assertEquals("java.lang.Comparable<T>", tp.getInterfaceBounds().get(0).toString());
        assertEquals("java.io.Serializable", tp.getInterfaceBounds().get(1).toString());
    }

    @Test
    void testExtendingGenericClass() {
        buildWorld("ExtendingGenericClass");
        JClass clz = getClass("ExtendingGenericClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: ExtendingGenericClass<E> extends AbstractList<E>
        assertEquals(1, gSig.getTypeParams().size());
        assertEquals("E", gSig.getTypeParams().get(0).getTypeName());
        ClassTypeGSignature superClass = gSig.getSuperClass();
        assertNotNull(superClass);
        assertEquals("java.util.AbstractList<E>", superClass.toString());
    }

    @Test
    void testRecursiveBoundClass() {
        buildWorld("RecursiveBoundClass");
        JClass clz = getClass("RecursiveBoundClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: RecursiveBoundClass<T extends RecursiveBoundClass<T>>
        List<TypeParameter> typeParams = gSig.getTypeParams();
        assertEquals(1, typeParams.size());
        TypeParameter tp = typeParams.get(0);
        assertEquals("T", tp.getTypeName());
        assertInstanceOf(ClassTypeGSignature.class, tp.getClassBound());
        assertEquals("RecursiveBoundClass<T>", tp.getClassBound().toString());
    }

    @Test
    void testGenericInterface() {
        buildWorld("GenericInterface");
        JClass clz = getClass("GenericInterface");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // interface: GenericInterface<T>
        assertTrue(clz.isInterface());
        assertEquals(1, gSig.getTypeParams().size());
        assertEquals("T", gSig.getTypeParams().get(0).getTypeName());
    }

    @Test
    void testGenericInterfaceImpl() {
        buildWorld("GenericInterfaceImpl");
        JClass clz = getClass("GenericInterfaceImpl");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: GenericInterfaceImpl<T> implements GenericInterface<T>
        assertEquals(1, gSig.getTypeParams().size());
        assertEquals("T", gSig.getTypeParams().get(0).getTypeName());
        List<ClassTypeGSignature> superInterfaces = gSig.getSuperInterfaces();
        assertEquals(1, superInterfaces.size());
        assertEquals("GenericInterface<T>", superInterfaces.get(0).toString());
    }

    @Test
    void testMultipleInterfacesClass() {
        buildWorld("MultipleInterfacesClass");
        JClass clz = getClass("MultipleInterfacesClass");
        ClassGSignature gSig = clz.getGSignature();
        assertNotNull(gSig);

        // class: MultipleInterfacesClass<T> implements Comparable<T>, Serializable
        assertEquals(1, gSig.getTypeParams().size());
        assertEquals("T", gSig.getTypeParams().get(0).getTypeName());
        List<ClassTypeGSignature> superInterfaces = gSig.getSuperInterfaces();
        assertEquals(2, superInterfaces.size());
        assertEquals("java.lang.Comparable<T>", superInterfaces.get(0).toString());
        assertEquals("java.io.Serializable", superInterfaces.get(1).toString());
    }

    @Test
    void testGenericMethod() {
        buildWorld("GenericMethod");
        JClass clz = getClass("GenericMethod");

        // method: <T> T identity(T value)
        JMethod identity = clz.getDeclaredMethod("identity");
        assertNotNull(identity);
        MethodGSignature identitySig = identity.getGSignature();
        assertNotNull(identitySig);
        assertEquals(1, identitySig.getTypeParams().size());
        assertEquals("T", identitySig.getTypeParams().get(0).getTypeName());

        // method: <T extends Number> T boundedIdentity(T value)
        JMethod boundedIdentity = clz.getDeclaredMethod("boundedIdentity");
        assertNotNull(boundedIdentity);
        MethodGSignature boundedSig = boundedIdentity.getGSignature();
        assertNotNull(boundedSig);
        TypeParameter tp = boundedSig.getTypeParams().get(0);
        assertEquals("T", tp.getTypeName());
        assertInstanceOf(ClassTypeGSignature.class, tp.getClassBound());
        assertEquals("java.lang.Number", tp.getClassBound().toString());

        // method: <K, V> V getFromPair(K key, V value)
        JMethod getFromPair = clz.getDeclaredMethod("getFromPair");
        assertNotNull(getFromPair);
        MethodGSignature pairSig = getFromPair.getGSignature();
        assertNotNull(pairSig);
        assertEquals(2, pairSig.getTypeParams().size());
        assertEquals("K", pairSig.getTypeParams().get(0).getTypeName());
        assertEquals("V", pairSig.getTypeParams().get(1).getTypeName());

        // method: <T> List<T> toList(T value)
        JMethod toList = clz.getDeclaredMethod("toList");
        assertNotNull(toList);
        MethodGSignature toListSig = toList.getGSignature();
        assertNotNull(toListSig);
        TypeGSignature toListResultSig = toListSig.getResultSignature();
        assertInstanceOf(ClassTypeGSignature.class, toListResultSig);
        assertEquals("java.util.List<T>", toListResultSig.toString());

        // method: void consumeList(List<String> list)
        JMethod consumeList = clz.getDeclaredMethod("consumeList");
        assertNotNull(consumeList);
        MethodGSignature consumeSig = consumeList.getGSignature();
        assertNotNull(consumeSig);
        assertEquals(1, consumeSig.getParameterSigs().size());
        TypeGSignature consumeParamSig = consumeSig.getParameterSigs().get(0);
        assertInstanceOf(ClassTypeGSignature.class, consumeParamSig);
        assertEquals("java.util.List<java.lang.String>", consumeParamSig.toString());

        // method: <E extends Exception> void throwsGeneric() throws E
        JMethod throwsGeneric = clz.getDeclaredMethod("throwsGeneric");
        assertNotNull(throwsGeneric);
        MethodGSignature throwsSig = throwsGeneric.getGSignature();
        assertNotNull(throwsSig);
        assertEquals(1, throwsSig.getTypeParams().size());
        TypeParameter throwsTp = throwsSig.getTypeParams().get(0);
        assertEquals("E", throwsTp.getTypeName());
        assertInstanceOf(ClassTypeGSignature.class, throwsTp.getClassBound());
        assertEquals("java.lang.Exception", throwsTp.getClassBound().toString());
        assertEquals(1, throwsSig.getThrowsSigs().size());
        TypeVariableGSignature throwsExSig =
                assertInstanceOf(TypeVariableGSignature.class, throwsSig.getThrowsSigs().get(0));
        assertEquals("E", throwsExSig.getTypeName());
    }

    @Test
    void testGenericField() {
        buildWorld("GenericField");
        JClass clz = getClass("GenericField");

        // field: T value
        JField valueField = clz.getDeclaredField("value");
        assertNotNull(valueField);
        ReferenceTypeGSignature valueSig = valueField.getGSignature();
        TypeVariableGSignature tvSig = assertInstanceOf(TypeVariableGSignature.class, valueSig);
        assertEquals("T", tvSig.getTypeName());

        // field: List<T> list
        JField listField = clz.getDeclaredField("list");
        assertNotNull(listField);
        ReferenceTypeGSignature listSig = listField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, listSig);
        assertEquals("java.util.List<T>", listSig.toString());

        // field: Map<String, T> map
        JField mapField = clz.getDeclaredField("map");
        assertNotNull(mapField);
        ReferenceTypeGSignature mapSig = mapField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, mapSig);
        assertEquals("java.util.Map<java.lang.String, T>", mapSig.toString());

        // field: List<String> stringList
        JField stringListField = clz.getDeclaredField("stringList");
        assertNotNull(stringListField);
        ReferenceTypeGSignature stringListSig = stringListField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, stringListSig);
        assertEquals("java.util.List<java.lang.String>", stringListSig.toString());

        // field: Map<String, List<T>> nestedMap
        JField nestedMapField = clz.getDeclaredField("nestedMap");
        assertNotNull(nestedMapField);
        ReferenceTypeGSignature nestedMapSig = nestedMapField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, nestedMapSig);
        assertEquals("java.util.Map<java.lang.String, java.util.List<T>>", nestedMapSig.toString());
    }

    @Test
    void testWildcardClass() {
        buildWorld("WildcardClass");
        JClass clz = getClass("WildcardClass");

        // field: List<?> unbounded
        JField unboundedField = clz.getDeclaredField("unbounded");
        assertNotNull(unboundedField);
        ReferenceTypeGSignature unboundedSig = unboundedField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, unboundedSig);
        assertEquals("java.util.List<?>", unboundedSig.toString());

        // field: List<? extends Number> upperBounded
        JField upperField = clz.getDeclaredField("upperBounded");
        assertNotNull(upperField);
        ReferenceTypeGSignature upperSig = upperField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, upperSig);
        assertEquals("java.util.List<? extends java.lang.Number>", upperSig.toString());

        // field: List<? super Integer> lowerBounded
        JField lowerField = clz.getDeclaredField("lowerBounded");
        assertNotNull(lowerField);
        ReferenceTypeGSignature lowerSig = lowerField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, lowerSig);
        assertEquals("java.util.List<? super java.lang.Integer>", lowerSig.toString());
    }

    @Test
    void testNestedGenericClass() {
        buildWorld("NestedGenericClass");
        JClass clz = getClass("NestedGenericClass");

        // field: Map<String, List<T>> mapOfLists
        JField mapOfListsField = clz.getDeclaredField("mapOfLists");
        assertNotNull(mapOfListsField);
        ReferenceTypeGSignature mapOfListsSig = mapOfListsField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, mapOfListsSig);
        assertEquals("java.util.Map<java.lang.String, java.util.List<T>>", mapOfListsSig.toString());

        // field: List<Map<String, T>> listOfMaps
        JField listOfMapsField = clz.getDeclaredField("listOfMaps");
        assertNotNull(listOfMapsField);
        ReferenceTypeGSignature listOfMapsSig = listOfMapsField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, listOfMapsSig);
        assertEquals("java.util.List<java.util.Map<java.lang.String, T>>", listOfMapsSig.toString());

        // field: Map<T, Map<String, List<T>>> deeplyNested
        JField deeplyNestedField = clz.getDeclaredField("deeplyNested");
        assertNotNull(deeplyNestedField);
        ReferenceTypeGSignature deeplyNestedSig = deeplyNestedField.getGSignature();
        assertInstanceOf(ClassTypeGSignature.class, deeplyNestedSig);
        assertEquals("java.util.Map<T, java.util.Map<java.lang.String, java.util.List<T>>>",
                deeplyNestedSig.toString());

        // method: <K> Map<K, List<T>> createMap(K key)
        JMethod createMap = clz.getDeclaredMethod("createMap");
        assertNotNull(createMap);
        MethodGSignature createMapSig = createMap.getGSignature();
        assertNotNull(createMapSig);
        assertEquals(1, createMapSig.getTypeParams().size());
        assertEquals("K", createMapSig.getTypeParams().get(0).getTypeName());
        TypeGSignature createMapResultSig = createMapSig.getResultSignature();
        assertInstanceOf(ClassTypeGSignature.class, createMapResultSig);
        assertEquals("java.util.Map<K, java.util.List<T>>", createMapResultSig.toString());
    }

    @Test
    void testGenericInnerClass() {
        buildWorld("GenericInnerClass");
        ClassHierarchy hierarchy = World.get().getClassHierarchy();

        // class: GenericInnerClass<T>.Inner<U>
        JClass innerClass = hierarchy.getClass("GenericInnerClass$Inner");
        assertNotNull(innerClass);
        ClassGSignature innerSig = innerClass.getGSignature();
        assertNotNull(innerSig);
        assertEquals(1, innerSig.getTypeParams().size());
        assertEquals("U", innerSig.getTypeParams().get(0).getTypeName());

        // Verify inner class field referencing outer class type parameter T
        // field: T outerRef
        JField outerRefField = innerClass.getDeclaredField("outerRef");
        assertNotNull(outerRefField);
        TypeVariableGSignature outerRefSig =
                assertInstanceOf(TypeVariableGSignature.class, outerRefField.getGSignature());
        assertEquals("T", outerRefSig.getTypeName());

        // field: U innerValue (inner class's own type parameter)
        JField innerValueField = innerClass.getDeclaredField("innerValue");
        assertNotNull(innerValueField);
        TypeVariableGSignature innerValueSig =
                assertInstanceOf(TypeVariableGSignature.class, innerValueField.getGSignature());
        assertEquals("U", innerValueSig.getTypeName());

        // class: GenericInnerClass.StaticInner<S>
        JClass staticInner = hierarchy.getClass("GenericInnerClass$StaticInner");
        assertNotNull(staticInner);
        ClassGSignature staticSig = staticInner.getGSignature();
        assertNotNull(staticSig);
        assertEquals(1, staticSig.getTypeParams().size());
        assertEquals("S", staticSig.getTypeParams().get(0).getTypeName());
    }

    @Test
    void testGenericArrayClass() {
        buildWorld("GenericArrayClass");
        JClass clz = getClass("GenericArrayClass");

        // field: T[] array
        JField arrayField = clz.getDeclaredField("array");
        assertNotNull(arrayField);
        ArrayTypeGSignature arraySig =
                assertInstanceOf(ArrayTypeGSignature.class, arrayField.getGSignature());
        assertEquals(1, arraySig.getDimensions());
        TypeVariableGSignature baseSig =
                assertInstanceOf(TypeVariableGSignature.class, arraySig.getBaseTypeGSignature());
        assertEquals("T", baseSig.getTypeName());

        // field: List<T>[] listArray
        JField listArrayField = clz.getDeclaredField("listArray");
        assertNotNull(listArrayField);
        ArrayTypeGSignature listArraySig =
                assertInstanceOf(ArrayTypeGSignature.class, listArrayField.getGSignature());
        assertEquals(1, listArraySig.getDimensions());
        TypeGSignature listArrayBaseSig = listArraySig.getBaseTypeGSignature();
        assertInstanceOf(ClassTypeGSignature.class, listArrayBaseSig);
        assertEquals("java.util.List<T>", listArrayBaseSig.toString());

        // field: T[][] twoDArray
        JField twoDArrayField = clz.getDeclaredField("twoDArray");
        assertNotNull(twoDArrayField);
        ArrayTypeGSignature twoDArraySig =
                assertInstanceOf(ArrayTypeGSignature.class, twoDArrayField.getGSignature());
        assertEquals(2, twoDArraySig.getDimensions());
        TypeVariableGSignature twoDBaseSig =
                assertInstanceOf(TypeVariableGSignature.class, twoDArraySig.getBaseTypeGSignature());
        assertEquals("T", twoDBaseSig.getTypeName());

        // method: <E> E[] toArray(E[] a)
        JMethod toArray = clz.getDeclaredMethod("toArray");
        assertNotNull(toArray);
        MethodGSignature toArraySig = toArray.getGSignature();
        assertNotNull(toArraySig);
        assertEquals(1, toArraySig.getTypeParams().size());
        assertEquals("E", toArraySig.getTypeParams().get(0).getTypeName());
        ArrayTypeGSignature resultSig =
                assertInstanceOf(ArrayTypeGSignature.class, toArraySig.getResultSignature());
        assertEquals(1, resultSig.getDimensions());
        TypeVariableGSignature resultBaseSig =
                assertInstanceOf(TypeVariableGSignature.class, resultSig.getBaseTypeGSignature());
        assertEquals("E", resultBaseSig.getTypeName());
    }

    @Test
    void testNonGenericClass() {
        buildWorld("NonGenericClass");
        JClass clz = getClass("NonGenericClass");

        // class: NonGenericClass (non-generic class should have null signature)
        ClassGSignature gSig = clz.getGSignature();
        assertNull(gSig, "Non-generic class should have null generic signature");

        // field: String value (non-generic field should have null signature)
        JField field = clz.getDeclaredField("value");
        assertNotNull(field);
        assertNull(field.getGSignature(), "Non-generic field should have null generic signature");

        // method: String getValue() (non-generic method should have null signature)
        JMethod method = clz.getDeclaredMethod("getValue");
        assertNotNull(method);
        assertNull(method.getGSignature(), "Non-generic method should have null generic signature");
    }
}
