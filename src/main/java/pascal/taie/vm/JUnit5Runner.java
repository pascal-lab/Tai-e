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

package pascal.taie.vm;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JUnit5Runner {
    static String TEST = "Lorg/junit/jupiter/api/Test;";

    public static void run(JClass testEntry, int timeOut) {
        if (testEntry.isAbstract()) {
            return;
        }
        JMethod init = testEntry.getDeclaredMethod(MethodNames.INIT);
        assert init != null;
        if (init.getParamCount() != 0) {
            System.out.println("Can not handle: " + testEntry);
            return;
        }

        for (JMethod m : testEntry.getDeclaredMethods()) {
            if (!m.isNative() && !m.isAbstract()) {
                boolean found = m.getAnnotations()
                        .stream()
                        .anyMatch(annotation -> annotation.getType().equals(TEST));
                if (found) {
                    runOne(init, m, timeOut);
                }
            }
        }
    }

    public static void runOne(JMethod init, JMethod method, int timeOut) {
        Future<Object> future = null;
        try {
            VM vm = new VM();
            Frame f = Frame.mkNewFrame();
            JObject thisObj = new JObject(vm, vm.loadClass(init.getDeclaringClass().getType()));
            f.getRegs().put(init.getIR().getThis(), thisObj);
            vm.execIR(init.getIR(), f);
            Frame f1 = Frame.mkNewFrame();
            f1.getRegs().put(method.getIR().getThis(), thisObj);
            Callable<Object> task = () -> vm.execIR(method.getIR(), f1);
            ExecutorService executor = Executors.newCachedThreadPool();
            future = executor.submit(task);
            Object result = future.get(timeOut, TimeUnit.SECONDS);
        } catch (Exception ex) {
            System.out.println("[INFO] failed, " + method + ", cause: " + ex);
//            throw new RuntimeException(ex);
        } finally {
            if (future != null) {
                future.cancel(true); // may or may not desire this
            }
        }
    }
}
