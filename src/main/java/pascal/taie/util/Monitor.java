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

/**
 * A comprehensive monitoring utility for tracking task execution metrics including
 * elapsed time, CPU usage, and memory consumption.
 *
 * <p>This class provides both instance-based and static methods for monitoring tasks.
 * It tracks the following metrics:
 * <ul>
 *   <li><b>Elapsed Time:</b> Total execution time in seconds</li>
 *   <li><b>CPU Usage:</b> Process CPU load at start, peak, and end of execution</li>
 *   <li><b>Memory Usage:</b> Heap and non-heap memory consumption at start, peak, and end</li>
 * </ul>
 *
 * <p>The monitor uses a scheduled executor to periodically sample CPU and memory usage
 * (every {@value #INTERVAL} second) to capture peak values during task execution.
 *
 * <p><b>Example usage (instance-based):</b>
 * <pre>{@code
 * Monitor monitor = new Monitor("MyTask");
 * monitor.start();
 * // ... perform task ...
 * monitor.stop();
 * System.out.println(monitor); // Prints all metrics
 * }</pre>
 *
 * <p><b>Example usage (static convenience method):</b>
 * <pre>{@code
 * Monitor.runAndCount(() -> {
 *     // ... perform task ...
 * }, "MyTask");
 * }</pre>
 *
 * <p>This class replaces the deprecated {@link Timer} class and provides enhanced
 * monitoring capabilities beyond simple time tracking.
 *
 * @see Timer
 */
public class Monitor {

    private static final Logger logger = LogManager.getLogger(Monitor.class);

    /**
     * Monitoring interval in seconds for periodic CPU and memory sampling.
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

    /**
     * Creates a new Monitor with the given name.
     *
     * @param name the name of this monitor, used in output messages
     */
    public Monitor(String name) {
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

    /**
     * Starts the monitoring process.
     *
     * <p>Records the start time and initial CPU/memory metrics, and begins
     * periodic sampling of CPU and memory usage to track peak values.
     * If the monitor is already running, this call has no effect.
     */
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

    /**
     * Stops the monitoring process.
     *
     * <p>Records the final elapsed time and end CPU/memory metrics, then
     * shuts down the periodic sampling scheduler. If the monitor is not
     * currently running, this call has no effect.
     */
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

    /**
     * Returns the total elapsed time in seconds.
     *
     * @return the elapsed time in seconds as a floating-point number
     */
    public float inSecond() {
        return elapsedTime / 1000F;
    }

    /**
     * Clears all monitoring data and resets the counting state.
     *
     * <p>This resets the elapsed time to zero and marks the monitor as not counting.
     */
    public void clear() {
        elapsedTime = 0;
        inCounting = false;
    }

    /**
     * Returns a string representation of the monitoring results.
     *
     * <p>The output includes the monitor name, elapsed time, CPU usage statistics
     * (start, peak, and end percentages), and memory usage statistics (start, peak,
     * and end in megabytes).
     *
     * @return a formatted string containing all monitoring metrics
     */
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
     * Runs a task, monitors its execution, logs the results, and returns the task's result.
     *
     * <p>This is a convenience method that creates a Monitor, starts it, executes the task,
     * stops monitoring, and logs the results at the specified logging level.
     *
     * @param <T>      the type of result returned by the task
     * @param task     the task to be executed
     * @param taskName the name of the task for logging purposes
     * @param level    the logging level for the monitoring results
     * @return the result of the task execution
     */
    public static <T> T runAndCount(Supplier<T> task, String taskName, Level level) {
        logger.info("[{}] starts ...", taskName);
        Monitor monitor = new Monitor(taskName);
        monitor.start();
        T result = task.get();
        monitor.stop();
        logger.log(level, monitor);
        return result;
    }

    /**
     * Runs a task, monitors its execution, and logs the results at INFO level.
     *
     * <p>This is a convenience method that creates a Monitor, starts it, executes the task,
     * stops monitoring, and logs the results at INFO level.
     *
     * @param task     the task to be executed
     * @param taskName the name of the task for logging purposes
     */
    public static void runAndCount(Runnable task, String taskName) {
        runAndCount(task, taskName, Level.INFO);
    }

    /**
     * Runs a task, monitors its execution, and logs the results at the specified level.
     *
     * <p>This is a convenience method that creates a Monitor, starts it, executes the task,
     * stops monitoring, and logs the results at the specified logging level.
     *
     * @param task     the task to be executed
     * @param taskName the name of the task for logging purposes
     * @param level    the logging level for the monitoring results
     */
    public static void runAndCount(Runnable task, String taskName, Level level) {
        runAndCount(() -> {
            task.run();
            return null;
        }, taskName, level);
    }

    /**
     * Runs a task with a given time budget.
     *
     * <p>Executes the task in a separate thread with a timeout. If the task does not
     * complete within the specified time limit, the execution is terminated and the
     * program exits with status code 1.
     *
     * @param task    the task to be executed
     * @param seconds the time budget in seconds
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
