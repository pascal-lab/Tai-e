/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.jimple;

import org.junit.Assert;
import org.junit.Test;
import soot.Scene;
import soot.SootMethod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IRBuilderTest {

    private static final int THREADS = Runtime.getRuntime()
            .availableProcessors();
    private static final int REPEAT = 10;

    @Test
    public void testSimple() {
        Utils.test("analyzed/cspta", "Array", irBuilder -> {
            SootMethod arrayMain = Scene.v().getMethod(
                    "<Array: void main(java.lang.String[])>");
            System.out.println(irBuilder.getMethod(arrayMain));
        });
    }

    @Test
    public void testMultithread() {
        for (int i = 0; i < REPEAT; ++i) {
            System.out.println("Iteration: " + i);
            Utils.test("analyzed/cspta", "Array", irBuilder -> {
                SootMethod arrayMain = Scene.v().getMethod(
                        "<Array: void main(java.lang.String[])>");
                ExecutorService service = Executors.newFixedThreadPool(THREADS);
                Set<Integer> sizeSet = ConcurrentHashMap.newKeySet();
                for (int t = 0; t < THREADS; ++t) {
                    service.execute(() -> {
                        int size = irBuilder.getMethod(arrayMain)
                                .getStatements().size();
//                        System.out.println("size: " + size);
                        sizeSet.add(size);
                    });
                }
                service.shutdown();
                try {
                    service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    Assert.assertEquals(1, sizeSet.size());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
