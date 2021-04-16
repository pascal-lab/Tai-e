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

package pascal.taie.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class Timer {

    private static final Logger logger = LogManager.getLogger(Timer.class);
    
    private final String name;

    private long elapsedTime = 0;

    private long startTime;

    private boolean inCounting = false;

    public Timer(String name) {
        this.name = name;
    }

    public void start() {
        if (!inCounting) {
            inCounting = true;
            startTime = System.currentTimeMillis();
        }
    }

    public void stop() {
        if (inCounting) {
            elapsedTime += System.currentTimeMillis() - startTime;
            inCounting = false;
        }
    }

    public float inSecond() {
        return elapsedTime / 1000F;
    }

    public void clear() {
        elapsedTime = 0;
        inCounting = false;
    }

    @Override
    public String toString() {
        return String.format("(%s) elapsed time: %.2fs",
                name, inSecond());
    }

    /**
     * Execute a task, log the elapsed time, and return the result.
     * @param message message of the task
     * @param task task to be executed
     */
    public static <T> T executeAndCount(String message, Supplier<T> task) {
        Timer timer = new Timer(message);
        timer.start();
        T result = task.get();
        timer.stop();
        logger.info(timer);
        return result;
    }

    /**
     * Execute a task and log the elapsed time.
     * @param message message of the task
     * @param task task to be executed
     */
    public static void executeAndCount(String message, Runnable task) {
        Timer timer = new Timer(message);
        timer.start();
        task.run();
        timer.stop();
        logger.info(timer);
    }
}
