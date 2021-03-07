/*
 * Tai-e: A Program Analysis Framework for Java
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

package pascal.taie.frontend.soot;

import org.junit.Test;
import pascal.taie.ir.NewIR;
import pascal.taie.java.World;
import pascal.taie.java.classes.JClass;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IRTest {

    private static final List<String> targets
            = Collections.singletonList("AllInOne");

    private static void initWorld(String mainClass) {
        String[] args = new String[]{
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "analyzed/ir",
                mainClass
        };
        TestUtils.buildWorld(args);
    }

    @Test
    public void testIRBuilder() {
        targets.forEach(main -> {
            initWorld(main);
            JClass mainClass = World.getMainMethod().getDeclaringClass();
            mainClass.getDeclaredMethods().forEach(m -> {
                System.out.println(m);
                NewIR ir = m.getNewIR();
                System.out.println("Parameters:");
                System.out.println(ir.getParams()
                        .stream()
                        .map(p -> p.getType() + " " + p)
                        .collect(Collectors.joining(", ")));
                System.out.println("Variables:");
                ir.getVars().forEach(v ->
                        System.out.println(v.getType() + " " + v));
                System.out.println("Statements:");
                ir.getStmts().forEach(System.out::println);
                System.out.println();
            });
            System.out.println("------------------------------\n");
        });
    }
}
