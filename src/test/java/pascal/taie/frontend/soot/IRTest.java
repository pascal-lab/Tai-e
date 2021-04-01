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

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.JClass;

import java.util.Collections;
import java.util.List;

public class IRTest {

    private static final List<String> targets
            = Collections.singletonList("AllInOne");

    private static void initWorld(String mainClass) {
        Main.buildWorld(new String[]{
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "test-resources/ir",
                mainClass
        });
    }

    @Test
    public void testIRBuilder() {
        // This will enable PackManager run several BodyTransformers
        // to optimize Jimple body.
        System.setProperty("ENABLE_JIMPLE_OPT", "true");

        targets.forEach(main -> {
            initWorld(main);
            JClass mainClass = World.getMainMethod().getDeclaringClass();
            mainClass.getDeclaredMethods().forEach(m ->
                    IRPrinter.print(m.getIR(), System.out));
            System.out.println("------------------------------\n");
        });
    }
}
