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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
     * Runs a task, log the elapsed time, and return the result.
     *
     * @param task    task to be executed
     * @param message message of the task
     */
    public static <T> T runAndCount(Supplier<T> task, String message) {
        Timer timer = new Timer(message);
        timer.start();
        T result = task.get();
        timer.stop();
        logger.info(timer);
        return result;
    }

    /**
     * Runs a task and log the elapsed time.
     *
     * @param task    task to be executed
     * @param message message of the task
     */
    public static void runAndCount(Runnable task, String message) {
        runAndCount(task, message, Level.INFO);
    }

    public static void runAndCount(Runnable task, String message, Level level) {
        Timer timer = new Timer(message);
        timer.start();
        task.run();
        timer.stop();
        logger.log(level, timer);
    }

    /**
     * Runs a task with given time budget.
     */
    public static void runWithTimeout(Runnable task, long seconds) {
        Duration timeout = Duration.ofSeconds(seconds);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> handler = executor.submit(task);
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
