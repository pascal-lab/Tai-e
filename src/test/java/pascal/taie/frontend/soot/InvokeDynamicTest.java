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

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.stream.Stream;

public class InvokeDynamicTest {

    private static final boolean isPrintIR = false;

    @Test
    public void testFunction() {
        final String main = "Function";
        Main.buildWorld("-pp", "-cp", "test-resources/ir", "-m", main);
        JClass mainClass = World.getClassHierarchy().getClass(main);
        JMethod mainMethod = mainClass.getDeclaredMethod("main");
        printIR(mainMethod.getIR());
        extractInvokeDynamics(mainMethod.getIR()).forEach(
                indy -> Assert.assertEquals("apply", indy.getMethodName()));
    }

    @Test
    public void testInterface() {
        final String main = "Interface";
        Main.buildWorld("-pp", "-cp", "test-resources/ir", "-m", main);
        JClass mainClass = World.getClassHierarchy().getClass(main);
        JMethod mainMethod = mainClass.getDeclaredMethod("main");
        printIR(mainMethod.getIR());
        extractInvokeDynamics(mainMethod.getIR()).forEach(indy -> {
            if (indy.getMethodName().equals("test")) {
                Assert.assertEquals("java.util.function.Predicate",
                        indy.getType().getName());
            }
            if (indy.getMethodName().equals("accept")) {
                Assert.assertEquals("java.util.function.Consumer",
                        indy.getType().getName());
            }
        });
    }

    @Test
    public void testMultiStatement() {
        final String main = "MultiStatement";
        Main.buildWorld("-pp", "-cp", "test-resources/ir", "-m", main);
        JClass mainClass = World.getClassHierarchy().getClass(main);
        JMethod mainMethod = mainClass.getDeclaredMethod("main");
        printIR(mainMethod.getIR());
        extractInvokeDynamics(mainMethod.getIR()).forEach(indy -> {
            MethodRef bootstrapMethodRef = indy.getBootstrapMethodRef();
            Assert.assertEquals("metafactory",
                    bootstrapMethodRef.getName());
        });
        printIR(mainClass.getDeclaredMethod("getValue").getIR());
    }

    @Test
    public void testWithArgs() {
        final String main = "WithArgs";
        Main.buildWorld("-pp", "-cp", "test-resources/ir", "-m", main);
        JClass mainClass = World.getClassHierarchy().getClass(main);
        JMethod mainMethod = mainClass.getDeclaredMethod("main");
        printIR(mainMethod.getIR());
        extractInvokeDynamics(mainMethod.getIR()).forEach(indy ->
                Assert.assertEquals("actionPerformed",
                        indy.getMethodName())
        );
    }

    @Test
    public void testCapture() {
        final String main = "Capture";
        Main.buildWorld("-pp", "-cp", "test-resources/ir", "-m", main);
        JClass mainClass = World.getClassHierarchy().getClass(main);
        mainClass.getDeclaredMethods().forEach(m -> printIR(m.getIR()));
    }

    private static Stream<InvokeDynamic> extractInvokeDynamics(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(s -> s.getInvokeExp() instanceof InvokeDynamic)
                .map(s -> (InvokeDynamic) s.getInvokeExp());
    }

    private static void printIR(IR ir) {
        if (isPrintIR) {
            IRPrinter.print(ir, System.out);
        }
    }
}
