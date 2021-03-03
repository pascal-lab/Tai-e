/*
 * Tai-e - A Program Analysis Framework for Java
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
import pascal.taie.java.classes.FieldRef;
import pascal.taie.java.classes.FieldResolutionFailedException;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.types.Type;

import java.util.Arrays;

public class HierarchyTest {

    private static ClassHierarchy hierarchy;

    private static TypeManager typeManager;

    @BeforeClass
    public static void initTypeManager() {
        TestUtils.buildWorld(new String[]{
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "analyzed/java",
                "Hierarchy"
        });
        hierarchy = World.get().getClassHierarchy();
        typeManager = World.get().getTypeManager();
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
        JClass superclass = hierarchy.getClass(sup);
        JClass subclass = hierarchy.getClass(sub);
        Assert.assertTrue(hierarchy.isSubclass(superclass, subclass));
    }

    private static void expectedNotSubclass(String sup, String sub) {
        JClass superclass = hierarchy.getClass(sup);
        JClass subclass = hierarchy.getClass(sub);
        Assert.assertFalse(hierarchy.isSubclass(superclass, subclass));
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
     * @param refClass declaring class of the reference
     * @param refName field name of the reference
     * @param declaringClass expected declaring class of the resolved field
     */
    private static void testResolveField(
            String refClass, String refName, String declaringClass) {
        JClass refJClass = hierarchy.getClass(refClass);
        JClass declaringJClass = hierarchy.getClass(declaringClass);
        Type refType = typeManager.getClassType("java.lang.String");
        FieldRef fieldRef = FieldRef.get(refJClass, refName, refType);
        JField field = fieldRef.resolve();
        Assert.assertEquals(declaringJClass, field.getDeclaringClass());
    }

    // ---------- Test method resolution resolveMethod()  ----------

    /**
     * Find methods in current class.
     */
    @Test
    public void testResolveMethod1() {
        testResolveMethod("E", "foo", "E", typeManager.getIntType());
        testResolveMethod("E", "<init>", "E");
    }

    /**
     * Find methods in superclasses.
     */
    @Test
    public void testResolveMethod2() {
        testResolveMethod("E", "foo", "C", typeManager.getLongType());
        testResolveMethod("E", "bar", "C");
        testResolveMethod("E", "hashCode", typeManager.getIntType(),
                "java.lang.Object");
        // The priority of superclasses is higher than superinterfaces
        testResolveMethod("G", "baz", "C", typeManager.getBooleanType());
    }

    /**
     * Find methods in superinterfaces.
     */
    @Test
    public void testResolveMethod3() {
        testResolveMethod("IIII", "biu", "I", typeManager.getClassType("I"));
        testResolveMethod("G", "biubiu", "IIII",
                typeManager.getClassType("IIII"));
        testResolveMethod("G", "biu", "I", typeManager.getClassType("I"));
    }

    /**
     * Find method in superclass's superinterface.
     */
    @Test
    public void testResolveMethod4() {
        testResolveMethod("H", "biu", "I", typeManager.getClassType("I"));
    }

    /**
     * Test resolveMethod() with specified class and name.
     * The declaring class of the resolved method should be the same
     * as the given expected class.
     * @param refClass declaring class of the reference
     * @param refName method name of the reference
     * @param declaringClass expected declaring class of the resolved method
     * @param returnType returnType of the reference
     * @param parameterTypes parameter types of the reference
     */
    private static void testResolveMethod(
            String refClass, String refName, Type returnType,
            String declaringClass, Type... parameterTypes) {
        JClass refJClass = hierarchy.getClass(refClass);
        JClass declaringJClass = hierarchy.getClass(declaringClass);
        MethodRef methodRef = MethodRef.get(refJClass, refName,
                Arrays.asList(parameterTypes), returnType);
        JMethod method = methodRef.resolve();
        Assert.assertEquals(declaringJClass, method.getDeclaringClass());
    }

    private static void testResolveMethod(
            String refClass, String refName, String declaringClass,
            Type... parameterTypes) {
        testResolveMethod(refClass, refName, typeManager.getVoidType(),
                declaringClass, parameterTypes);
    }
}
