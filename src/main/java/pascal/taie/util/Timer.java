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

import com.sun.management.OperatingSystemMXBean;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class Timer {

    private static final Logger logger = LogManager.getLogger(Timer.class);

    /**
     * Monitoring interval in seconds
     */
    private static final int INTERVAL = 1;

    private final String name;

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ScheduledExecutorService scheduler;

    // Time tracking
    private long startTime;
    private long elapsedTime = 0;

    // CPU and Memory tracking
    private double startCpuUsage;
    private double peakCpuUsage;
    private double endCpuUsage;
    private long startMemoryMB;
    private long peakMemoryMB;
    private long endMemoryMB;

    private boolean inCounting = false;

    public Timer(String name) {
        this.name = name;
        this.osBean = (OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, this.getClass().getName() + "[" + name + "]");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (!inCounting) {
            inCounting = true;
            startTime = System.currentTimeMillis();
            startCpuUsage = getCpuUsage();
            startMemoryMB = getMemoryUsedMB();
            // start up the scheduler
            scheduler.scheduleAtFixedRate(this::updatePeakValues,
                    0, INTERVAL, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (inCounting) {
            inCounting = false;
            elapsedTime += System.currentTimeMillis() - startTime;
            endCpuUsage = getCpuUsage();
            endMemoryMB = getMemoryUsedMB();
            // shut down the scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] elapsed time: %.2fs", name, inSecond()));
        sb.append(String.format("; CPU usage: start %.2f%%, peak %.2f%%, end %.2f%%",
                startCpuUsage * 100, peakCpuUsage * 100, endCpuUsage * 100));
        sb.append(String.format("; Memory usage: start %dMB, peak %dMB, end %dMB",
                startMemoryMB, peakMemoryMB, endMemoryMB));
        return sb.toString();
    }

    private void updatePeakValues() {
        double currentCpuUsage = getCpuUsage();
        if (currentCpuUsage > peakCpuUsage) {
            peakCpuUsage = currentCpuUsage;
        }
        long currentMemoryMB = getMemoryUsedMB();
        if (currentMemoryMB > peakMemoryMB) {
            peakMemoryMB = currentMemoryMB;
        }
    }

    private double getCpuUsage() {
        double processCpuUsage = osBean.getProcessCpuLoad();
        if (processCpuUsage < 0) {
            processCpuUsage = 0.0;
        }
        return processCpuUsage;
    }

    private long getMemoryUsedMB() {
        long heapMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long nonHeapMemoryUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        return (heapMemoryUsed + nonHeapMemoryUsed) / (1024 * 1024);
    }

    /**
     * Runs a task, log the elapsed time, and return the result.
     *
     * @param task     task to be executed
     * @param taskName name of the task
     */
    public static <T> T runAndCount(Supplier<T> task, String taskName, Level level) {
        logger.info("[{}] starts ...", taskName);
        Timer timer = new Timer(taskName);
        timer.start();
        T result = task.get();
        timer.stop();
        logger.log(level, timer);
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
