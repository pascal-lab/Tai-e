/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.soot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class IRBuilder implements pascal.taie.ir.IRBuilder {

    private static final Logger logger = LogManager.getLogger(IRBuilder.class);

    private final transient Converter converter;

    IRBuilder(Converter converter) {
        this.converter = converter;
    }

    @Override
    public IR buildIR(JMethod method) {
        try {
            return new MethodIRBuilder(method, converter).build();
        } catch (RuntimeException e) {
            if (e.getStackTrace()[0].getClassName().startsWith("soot")) {
                logger.warn("Soot front failed to build method body for {}," +
                        " constructs an empty IR instead", method);
                return new IRBuildHelper(method).buildEmpty();
            } else {
                throw e;
            }
        }
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
