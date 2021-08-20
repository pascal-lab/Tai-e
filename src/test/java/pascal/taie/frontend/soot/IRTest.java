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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class IRTest {

    private static final List<String> targets
            = Collections.singletonList("AllInOne");

    private static void buildWorld(String mainClass) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/basic", "-m", mainClass);
    }

    @BeforeClass
    public static void start() {
        // This will enable PackManager run several BodyTransformers
        // to optimize Jimple body.
        System.setProperty("ENABLE_JIMPLE_OPT", "true");
    }

    @AfterClass
    public static void clear() {
        System.clearProperty("ENABLE_JIMPLE_OPT");
    }

    @Test
    public void testIRBuilder() {
        targets.forEach(main -> {
            buildWorld(main);
            JClass mainClass = World.getClassHierarchy().getClass(main);
            mainClass.getDeclaredMethods()
                    .stream()
                    .sorted(Comparator.comparing(JMethod::toString))
                    .forEach(m ->
                            IRPrinter.print(m.getIR(), System.out));
            System.out.println("------------------------------\n");
        });
    }

    @Test
    public void testDefUse() {
        String main = "DefUse";
        buildWorld(main);
        JClass mainClass = World.getClassHierarchy().getClass(main);
        mainClass.getDeclaredMethods().forEach(m -> {
            System.out.println(m);
            m.getIR().forEach(stmt ->
                    System.out.printf("%s, def: %s, uses: %s%n",
                            stmt, stmt.getDef(), stmt.getUses()));
            System.out.println("--------------------");
        });
    }
}
