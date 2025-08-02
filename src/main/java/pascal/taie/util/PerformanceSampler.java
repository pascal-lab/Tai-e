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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.management.OperatingSystemMXBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance sampler for collecting system and JVM performance metrics during execution.
 * Supports automatic sampling at configurable intervals and outputs data in JSON format.
 */
public class PerformanceSampler {

    private static final Logger logger = LogManager.getLogger(PerformanceSampler.class);

    public static final String OUTPUT_FILE = "tai-e-performance.json";

    /**
     * Sampling interval in seconds
     */
    private static final int INTERVAL = 1;

    private final File outputFile;

    private final ScheduledExecutorService scheduler;

    private final OperatingSystemMXBean osBean;

    private final MemoryMXBean memoryBean;

    private final List<Sample> samples;

    private long startTime;

    private long finishTime;

    /**
     * Creates a new PerformanceSampler instance.
     */
    public PerformanceSampler(File outputDir) {
        this.outputFile = new File(outputDir, OUTPUT_FILE);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceSampler");
            t.setDaemon(true);
            return t;
        });
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.samples = new ArrayList<>();
    }

    /**
     * Starts performance sampling. Records start time and begins periodic sampling.
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(this::collectSample,
                0, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Stops performance sampling, records finish time, and saves results to JSON file.
     */
    public void stop() {
        this.finishTime = System.currentTimeMillis();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        saveToFile();
    }

    /**
     * Collects a single performance sample including CPU and memory usage.
     */
    private void collectSample() {
        try {
            long timestamp = System.currentTimeMillis();

            // Get CPU usage and handle negative values indicating unavailable data
            double processCpuUsage = osBean.getProcessCpuLoad();
            if (processCpuUsage < 0) {
                processCpuUsage = 0.0;
            }
            double totalProcessCpuUsage = osBean.getCpuLoad();
            if (totalProcessCpuUsage < 0) {
                totalProcessCpuUsage = 0.0;
            }

            // Get JVM process memory usage (heap + non-heap)
            long heapMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long nonHeapMemoryUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long processMemoryUsedMB = (heapMemoryUsed + nonHeapMemoryUsed) / (1024 * 1024);

            // Get total system memory usage
            long totalMemory = osBean.getTotalMemorySize();
            long freeMemory = osBean.getFreeMemorySize();
            long totalMemoryUsedMB = (totalMemory - freeMemory) / (1024 * 1024);

            Sample sample = new Sample(timestamp, processCpuUsage,
                    totalProcessCpuUsage, processMemoryUsedMB, totalMemoryUsedMB);

            synchronized (samples) {
                samples.add(sample);
            }
        } catch (Exception e) {
            // Log error but continue sampling
            logger.error("Error collecting performance sample: {}", e.getMessage());
        }
    }

    /**
     * Saves performance data to JSON file.
     */
    private void saveToFile() {
        logger.info("Saving performance report to: {}", outputFile);
        try {
            PerformanceReport report = new PerformanceReport();
            report.version = RuntimeInfoLogger.getVersion();
            report.commit = RuntimeInfoLogger.getCommit();
            report.operatingSystem = System.getProperty("os.name")
                    + " (" + System.getProperty("os.arch") + ")";
            report.javaRuntime = System.getProperty("java.vendor")
                    + " " + System.getProperty("java.runtime.name")
                    + " " + System.getProperty("java.runtime.version");
            report.username = System.getProperty("user.name");
            report.cpuCores = Runtime.getRuntime().availableProcessors();
            report.totalMemoryMB = osBean.getTotalMemorySize() / (1024 * 1024);
            report.startTime = startTime;
            report.finishTime = finishTime;

            synchronized (samples) {
                report.samples = new ArrayList<>(samples);
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(outputFile, report);
        } catch (IOException e) {
            logger.error("Failed to write performance report: {}", e.getMessage());
        }
    }

    /**
     * Main performance report structure for JSON serialization.
     */
    private static class PerformanceReport {
        @JsonProperty("version")
        public String version;

        @JsonProperty("commit")
        public String commit;

        @JsonProperty("operatingSystem")
        public String operatingSystem;

        @JsonProperty("javaRuntime")
        public String javaRuntime;

        @JsonProperty("username")
        public String username;

        @JsonProperty("cpuCores")
        public int cpuCores;

        @JsonProperty("totalMemoryMB")
        public long totalMemoryMB;

        @JsonProperty("startTime")
        public long startTime;

        @JsonProperty("finishTime")
        public Long finishTime;

        @JsonProperty("samples")
        public List<Sample> samples;
    }

    /**
     * Individual performance sample data point.
     */
    private record Sample(
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("processCpuUsage") double processCpuUsage,
            @JsonProperty("totalProcessCpuUsage") double totalProcessCpuUsage,
            @JsonProperty("processMemoryUsedMB") long processMemoryUsedMB,
            @JsonProperty("totalMemoryUsedMB") long totalMemoryUsedMB) {
    }

}
