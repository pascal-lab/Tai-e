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
package pascal.taie.analysis.bugfinder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.bugfinder.detector.DroppedException;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DroppedExceptionTest {

    @Test
    public void test(){
        Tests.test("DroppedException", "src/test/resources/bugfinder",
                DroppedException.ID);
    }
}