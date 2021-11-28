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

package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;

public class InterCPAliasTestFull {

    // Tests instance field
    @Test
    public void testInstanceField() {
        InterCPAliasTest.test("InstanceField");
    }

    @Test
    public void testMultiLoads() {
        InterCPAliasTest.test("MultiLoads");
    }

    @Test
    public void testMultiStores() {
        InterCPAliasTest.test("MultiStores");
    }

    @Test
    public void testMultiObjs() {
        InterCPAliasTest.test("MultiObjs");
    }

    @Test
    public void testInterprocedural() {
        InterCPAliasTest.test("Interprocedural");
    }

    @Test
    public void testInterprocedural2() {
        InterCPAliasTest.test("Interprocedural2");
    }

    @Test
    public void testInheritedField() {
        InterCPAliasTest.test("InheritedField");
    }

    @Test
    public void testFieldCorner() {
        InterCPAliasTest.test("FieldCorner");
    }

    // Tests static field
    @Test
    public void testStaticField() {
        InterCPAliasTest.test("StaticField");
    }

    @Test
    public void testStaticFieldMultiStores() {
        InterCPAliasTest.test("StaticFieldMultiStores");
    }

    // Tests array
    @Test
    public void testArray() {
        InterCPAliasTest.test("Array");
    }

    @Test
    public void testArrayField() {
        InterCPAliasTest.test("ArrayField");
    }

    @Test
    public void testArrayInter() {
        InterCPAliasTest.test("ArrayInter");
    }

    @Test
    public void testArrayInter2() {
        InterCPAliasTest.test("ArrayInter2");
    }

    @Test
    public void testArrayLoops() {
        InterCPAliasTest.test("ArrayLoops");
    }

    @Test
    public void testArrayCorner() {
        InterCPAliasTest.test("ArrayCorner");
    }

    // Other tests
    @Test
    public void testReference() {
        InterCPAliasTest.test("Reference");
    }

    @Test
    public void testObjSens() {
        InterCPAliasTest.test("ObjSens");
    }

    @Test
    public void testObjSens2() {
        InterCPAliasTest.test("ObjSens2");
    }

    @Test
    public void testArrayInField() {
        InterCPAliasTest.test("ArrayInField");
    }

    @Test
    public void testMaxPQ() {
        InterCPAliasTest.test("MaxPQ");
    }
}
