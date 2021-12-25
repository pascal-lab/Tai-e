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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Jimple-based pointer analysis IR builder.
 */
class IRBuilder implements pascal.taie.ir.IRBuilder {

    private static final Logger logger = LogManager.getLogger(IRBuilder.class);

    private final Converter converter;

    IRBuilder(Converter converter) {
        this.converter = converter;
    }

    @Override
    public IR buildIR(JMethod method) {
        return new MethodIRBuilder(method, converter).build();
    }

    /**
     * Builds IR for all methods in given class hierarchy.
     */
    @Override
    public void buildAll(ClassHierarchy hierarchy) {
        Timer timer = new Timer("Build IR for all methods");
        timer.start();
        int nThreads = Runtime.getRuntime().availableProcessors();
        // Group all methods by number of threads
        List<List<JMethod>> groups = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            groups.add(new ArrayList<>());
        }
        List<JClass> classes = hierarchy.allClasses().toList();
        int i = 0;
        for (JClass c : classes) {
            for (JMethod m : c.getDeclaredMethods()) {
                if (!m.isAbstract() || m.isNative()) {
                    groups.get(i++ % nThreads).add(m);
                }
            }
        }
        // Build IR for all methods in parallel
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (List<JMethod> group : groups) {
            service.execute(() -> group.forEach(JMethod::getIR));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timer.stop();
        logger.info(timer);
    }
}
