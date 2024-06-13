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
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.FieldResolutionFailedException;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.VoidType.VOID;

public class HierarchyTest {

    @BeforeAll
    public static void buildWorld() {
        Main.buildWorld("-cp", "src/test/resources/world", "--input-classes", "Hierarchy");
    }

    // ---------- Test subclass checking Subclass() ----------

    /**
     * Test interface and subinterfaces.
     */
    @Test
    void testSubclass1() {
        String i = "I", ii = "II", iii = "III", iiii = "IIII";
        expectedSubclass(i, iii);
        expectedSubclass(i, iiii);
        expectedNotSubclass(i, ii);
        expectedNotSubclass(ii, i);
        expectedNotSubclass(iii, i);
    }

    /**
     * Test interfaces and java.lang.Object.
     */
    @Test
    void testSubclass2() {
        String sObject = "java.lang.Object", i = "I", c = "C";
        expectedSubclass(sObject, i);
        expectedNotSubclass(i, sObject);
        expectedSubclass(sObject, c);
        expectedNotSubclass(c, sObject);
    }

    /**
     * Test interface and implementers
     */
    @Test
    void testSubclass3() {
        String i = "I", e = "E", f = "F", g = "G";
        expectedSubclass(i, e);
        expectedSubclass(i, f);
        expectedSubclass(i, g);
        expectedNotSubclass(e, i);
    }

    /**
     * Test class and subclasses.
     */
    @Test
    void testSubclass4() {
        String c = "C", d = "D", g = "G";
        expectedSubclass(c, d);
        expectedSubclass(c, g);
        expectedNotSubclass(d, c);
    }

    private static void expectedSubclass(String sup, String sub) {
        JClass superclass = getClass(sup);
        JClass subclass = getClass(sub);
        assertTrue(World.get().getClassHierarchy()
                .isSubclass(superclass, subclass));
    }

    private static void expectedNotSubclass(String sup, String sub) {
        JClass superclass = getClass(sup);
        JClass subclass = getClass(sub);
        assertFalse(World.get().getClassHierarchy()
                .isSubclass(superclass, subclass));
    }

    // ---------- Test field resolution resolveField()  ----------

    /**
     * Find field in current class.
     */
    @Test
    void testResolveField1() {
        testResolveField("E", "fe", "E");
    }

    /**
     * Find field in superclass.
     */
    @Test
    void testResolveField2() {
        testResolveField("G", "fc", "C");
    }

    /**
     * Find field in superinterface.
     */
    @Test
    void testResolveField3() {
        testResolveField("F", "fii", "II");
    }

    /**
     * Find field in superclass, the first matching field should be resolved.
     */
    @Test
    void testResolveField4() {
        testResolveField("G", "f", "E");
    }

    /**
     * Find non-exist field.
     */
    @Test
    void testResolveField5() {
        assertThrows(FieldResolutionFailedException.class,
                () -> testResolveField("G", "xxx", "G"));
    }

    /**
     * Resolve the same field twice.
     */
    @Test
    void testResolveField6() {
        testResolveField("G", "f", "E");
        testResolveField("G", "f", "E");
    }

    /**
     * Test resolveField() with specified class and field names.
     * The declaring class of the resolved field should be the same
     * as the given expected class.
     *
     * @param refClass       declaring class of the reference
     * @param refName        field name of the reference
     * @param declaringClass expected declaring class of the resolved field
     */
    private static void testResolveField(
            String refClass, String refName, String declaringClass) {
        JClass refJClass = getClass(refClass);
        JClass declaringJClass = getClass(declaringClass);
        Type refType = getClassType("java.lang.String");
        FieldRef fieldRef = FieldRef.get(refJClass, refName, refType, false);
        JField field = fieldRef.resolve();
        assertEquals(declaringJClass, field.getDeclaringClass());
    }

    // ---------- Test method resolution resolveMethod()  ----------

    /**
     * Find methods in current class.
     */
    @Test
    void testResolveMethod1() {
        testResolveMethod("E", "foo", "E", INT);
        testResolveMethod("E", "<init>", "E");
    }

    /**
     * Find methods in superclasses.
     */
    @Test
    void testResolveMethod2() {
        testResolveMethod("E", "foo", "C", LONG);
        testResolveMethod("E", "bar", "C");
        testResolveMethod("E", "hashCode", INT,
                "java.lang.Object");
        // The priority of superclasses is higher than superinterfaces
        testResolveMethod("G", "baz", "C", BOOLEAN);
    }

    /**
     * Find methods in superinterfaces.
     */
    @Test
    void testResolveMethod3() {
        testResolveMethod("IIII", "biu", "I", getClassType("I"));
        testResolveMethod("G", "biubiu", "IIII",
                getClassType("IIII"));
        testResolveMethod("G", "biu", "I", getClassType("I"));
    }

    /**
     * Find method in superclass's superinterface.
     */
    @Test
    void testResolveMethod4() {
        testResolveMethod("H", "biu", "I", getClassType("I"));
    }

    private static ClassType getClassType(String className) {
        return World.get().getTypeSystem().getClassType(className);
    }

    private static JClass getClass(String className) {
        return World.get().getClassHierarchy().getClass(className);
    }

    /**
     * Test resolveMethod() with specified class and name.
     * The declaring class of the resolved method should be the same
     * as the given expected class.
     *
     * @param refClass       declaring class of the reference
     * @param refName        method name of the reference
     * @param declaringClass expected declaring class of the resolved method
     * @param returnType     returnType of the reference
     * @param parameterTypes parameter types of the reference
     */
    static void testResolveMethod(
            String refClass, String refName, Type returnType,
            String declaringClass, Type... parameterTypes) {
        JClass refJClass = getClass(refClass);
        JClass declaringJClass = getClass(declaringClass);
        MethodRef methodRef = MethodRef.get(refJClass, refName,
                Arrays.asList(parameterTypes), returnType, false);
        JMethod method = methodRef.resolve();
        assertEquals(declaringJClass, method.getDeclaringClass());
    }

    static void testResolveMethod(
            String refClass, String refName, String declaringClass,
            Type... parameterTypes) {
        testResolveMethod(refClass, refName, VOID,
                declaringClass, parameterTypes);
    }

    // ---------- Test subclasses getAllSubclasses()  ----------

    /**
     * Test subclasses of class.
     */
    @Test
    void testSubclasses() {
        Collection<JClass> subclasses;
        JClass c = getClass("C");
        subclasses = getAllSubclasses(c);
        assertTrue(subclasses.contains(c));
    }

    /**
     * Test subclasses of interface.
     */
    @Test
    void testInterfaceSubclasses() {
        JClass i = getClass("I");
        Collection<JClass> subclasses = getAllSubclasses(i);

        assertTrue(subclasses.contains(getClass("IIII")));
        assertTrue(subclasses.contains(getClass("E")));
        assertTrue(subclasses.contains(getClass("G")));
        assertTrue(subclasses.contains(getClass("H")));

        assertFalse(subclasses.contains(getClass("II")));
        assertFalse(subclasses.contains(getClass("C")));
    }

    private static Collection<JClass> getAllSubclasses(JClass jclass) {
        return World.get().getClassHierarchy()
                .getAllSubclassesOf(jclass);
    }
}
