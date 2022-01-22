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

package pascal.taie.frontend.soot;

import org.junit.Before;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;

public class AnnotationTest {

    @Before
    public void setUp() throws Exception {
        Main.buildWorld("-pp", "-cp", "src/test/resources/basic", "-m", "Annotated");
    }

    @Test
    public void testAnnotation() {
        JClass main = World.get().getClassHierarchy().getClass("Annotated");
        AnnotationPrinter.print(main);
    }
}
