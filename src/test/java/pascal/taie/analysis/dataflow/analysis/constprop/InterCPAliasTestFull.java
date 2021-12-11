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

public class InterCPAliasTestFull extends InterCPAliasTest {

    // Tests instance field
    @Test
    public void testMultiLoads() {
        test("MultiLoads");
    }

    @Test
    public void testMultiObjs() {
        test("MultiObjs");
    }

    @Test
    public void testInterprocedural() {
        test("Interprocedural");
    }
    
    @Test
    public void testInheritedField() {
        test("InheritedField");
    }

    @Test
    public void testFieldCorner() {
        test("FieldCorner");
    }
    
    // Tests array
    @Test
    public void testArrayField() {
        test("ArrayField");
    }

    @Test
    public void testArrayInter() {
        test("ArrayInter");
    }
    
    @Test
    public void testArrayCorner() {
        test("ArrayCorner");
    }

    // Other tests
    @Test
    public void testReference() {
        test("Reference");
    }
    
    @Test
    public void testObjSens2() {
        test("ObjSens2");
    }

    @Test
    public void testArrayInField() {
        test("ArrayInField");
    }

    @Test
    public void testMaxPQ() {
        test("MaxPQ");
    }
}
