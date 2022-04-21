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
        return String.format("[%s] elapsed time: %.2fs",
                name, inSecond());
    }

    /**
     * Runs a task, log the elapsed time, and return the result.
     *
     * @param task     task to be executed
     * @param taskName name of the task
     */
    public static <T> T runAndCount(Supplier<T> task, String taskName, Level level) {
        logger.info("{} starts ...", taskName);
        Timer timer = new Timer(taskName);
        timer.start();
        T result = task.get();
        timer.stop();
        logger.log(level, "{} finishes, elapsed time: {}", taskName,
                String.format("%.2fs", timer.inSecond()));
        return result;
    }

    /**
     * Runs a task and log the elapsed time.
     *
     * @param task     task to be executed
     * @param taskName taskName of the task
     */
    public static void runAndCount(Runnable task, String taskName) {
        runAndCount(task, taskName, Level.INFO);
    }

    public static void runAndCount(Runnable task, String taskName, Level level) {
        runAndCount(() -> {
            task.run();
            return null;
        }, taskName, level);
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
