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

package panda.util;

import soot.LabeledUnitPrinter;
import soot.Unit;
import soot.jimple.GotoStmt;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SootUtils {

    // Suppresses default constructor, ensuring non-instantiability.
    private SootUtils() {
    }

    /**
     * Converts an Unit to its String representation.
     */
    public static String unitToString(LabeledUnitPrinter up, Unit unit) {
        StringBuilder sb = new StringBuilder();
        sb.append("L")
                .append(unit.getJavaSourceStartLineNumber())
                .append("{");
        String label = up.labels().get(unit);
        if (label != null) {
            sb.append(label).append(": ");
        }
        if (unit instanceof GotoStmt) {
            sb.append("goto ")
                    .append(up.labels().get(((GotoStmt) unit).getTarget()));
        } else {
            sb.append(unit);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Runs Soot with given time budget.
     */
    public static void runSootWithTimeout(String[] args, long seconds) {
        Duration timeout = Duration.ofSeconds(seconds);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        @SuppressWarnings("unchecked")
        Future<Void> handler = (Future<Void>)
                executor.submit(() -> soot.Main.main(args));
        try {
            handler.get(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
