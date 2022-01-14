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

import static pascal.taie.language.type.PrimitiveType.BOOLEAN;
import static pascal.taie.language.type.PrimitiveType.INT;
import static pascal.taie.language.type.PrimitiveType.LONG;
import static pascal.taie.language.type.VoidType.VOID;

public class HierarchyTest {

    @BeforeClass
    public static void buildWorld() {
        Main.buildWorld("-cp", "src/test/resources/basic", "-m", "Hierarchy");
    }

    // ---------- Test subclass checking Subclass() ----------

    /**
     * Test interface and subinterfaces.
     */
    @Test
    public void testSubclass1() {
        String I = "I", II = "II", III = "III", IIII = "IIII";
        expectedSubclass(I, III);
        expectedSubclass(I, IIII);
        expectedNotSubclass(I, II);
        expectedNotSubclass(II, I);
        expectedNotSubclass(III, I);
    }

    /**
     * Test interfaces and java.lang.Object.
     */
    @Test
    public void testSubclass2() {
        String Object = "java.lang.Object", I = "I", C = "C";
        expectedSubclass(Object, I);
        expectedNotSubclass(I, Object);
        expectedSubclass(Object, C);
        expectedNotSubclass(C, Object);
    }

    /**
     * Test interface and implementers
     */
    @Test
    public void testSubclass3() {
        String I = "I", E = "E", F = "F", G = "G";
        expectedSubclass(I, E);
        expectedSubclass(I, F);
        expectedSubclass(I, G);
        expectedNotSubclass(E, I);
    }

    /**
     * Test class and subclasses.
     */
    @Test
    public void testSubclass4() {
        String C = "C", D = "D", G = "G";
        expectedSubclass(C, D);
        expectedSubclass(C, G);
        expectedNotSubclass(D, C);
    }

    private static void expectedSubclass(String sup, String sub) {
        JClass superclass = getClass(sup);
        JClass subclass = getClass(sub);
        Assert.assertTrue(World.get().getClassHierarchy()
                .isSubclass(superclass, subclass));
    }

    private static void expectedNotSubclass(String sup, String sub) {
        JClass superclass = getClass(sup);
        JClass subclass = getClass(sub);
        Assert.assertFalse(World.get().getClassHierarchy()
                .isSubclass(superclass, subclass));
    }

    // ---------- Test field resolution resolveField()  ----------

    /**
     * Find field in current class.
     */
    @Test
    public void testResolveField1() {
        testResolveField("E", "fe", "E");
    }

    /**
     * Find field in superclass.
     */
    @Test
    public void testResolveField2() {
        testResolveField("G", "fc", "C");
    }

    /**
     * Find field in superinterface.
     */
    @Test
    public void testResolveField3() {
        testResolveField("F", "fii", "II");
    }

    /**
     * Find field in superclass, the first matching field should be resolved.
     */
    @Test
    public void testResolveField4() {
        testResolveField("G", "f", "E");
    }

    /**
     * Find non-exist field.
     */
    @Test(expected = FieldResolutionFailedException.class)
    public void testResolveField5() {
        testResolveField("G", "xxx", "G");
    }

    /**
     * Resolve the same field twice.
     */
    @Test
    public void testResolveField6() {
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
        Assert.assertEquals(declaringJClass, field.getDeclaringClass());
    }

    // ---------- Test method resolution resolveMethod()  ----------

    /**
     * Find methods in current class.
     */
    @Test
    public void testResolveMethod1() {
        testResolveMethod("E", "foo", "E", INT);
        testResolveMethod("E", "<init>", "E");
    }

    /**
     * Find methods in superclasses.
     */
    @Test
    public void testResolveMethod2() {
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
    public void testResolveMethod3() {
        testResolveMethod("IIII", "biu", "I", getClassType("I"));
        testResolveMethod("G", "biubiu", "IIII",
                getClassType("IIII"));
        testResolveMethod("G", "biu", "I", getClassType("I"));
    }

    /**
     * Find method in superclass's superinterface.
     */
    @Test
    public void testResolveMethod4() {
        testResolveMethod("H", "biu", "I", getClassType("I"));
    }

    private static ClassType getClassType(String className) {
        return World.get().getTypeManager().getClassType(className);
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
        Assert.assertEquals(declaringJClass, method.getDeclaringClass());
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
    public void testSubclasses() {
        Collection<JClass> subclasses;
        JClass C = getClass("C");
        subclasses = getAllSubclasses(C, true);
        Assert.assertTrue(subclasses.contains(C));
        subclasses = getAllSubclasses(C, false);
        Assert.assertFalse(subclasses.contains(C));
    }

    /**
     * Test subclasses of interface.
     */
    @Test
    public void testInterfaceSubclasses() {
        JClass I = getClass("I");
        Collection<JClass> subclasses = getAllSubclasses(I, true);

        Assert.assertTrue(subclasses.contains(getClass("IIII")));
        Assert.assertTrue(subclasses.contains(getClass("E")));
        Assert.assertTrue(subclasses.contains(getClass("G")));
        Assert.assertTrue(subclasses.contains(getClass("H")));

        Assert.assertFalse(subclasses.contains(getClass("II")));
        Assert.assertFalse(subclasses.contains(getClass("C")));
    }

    private static Collection<JClass> getAllSubclasses(
            JClass jclass, boolean selfInclude) {
        return World.get().getClassHierarchy()
                .getAllSubclassesOf(jclass, selfInclude);
    }
}
