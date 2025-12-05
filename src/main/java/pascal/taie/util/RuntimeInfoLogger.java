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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * A utility class for logging runtime information about the environment and the Tai-e application.
 * This includes details about the Java environment and versioning information for the Tai-e application.
 */
public class RuntimeInfoLogger {

    private static final Logger logger = LogManager.getLogger(RuntimeInfoLogger.class);

    private static final String[] ENV_KEYS = {
            "java.version",
            "java.version.date",
            "java.runtime.version",
            "java.vendor",
            "java.vendor.version",
            "os.name",
            "os.version",
            "os.arch",
    };

    private static final String BUILD_PROPERTY_FILE_PATH =
            "META-INF/tai-e-build.properties";

    private static final String VERSION_KEY = "version";

    private static final String COMMIT_KEY = "commit";

    private static final String UNKNOWN = "Unknown";

    /**
     * Logs runtime information, including environment details and Tai-e version/commit information.
     */
    public static void logRuntimeInfo() {
        logEnvInfo();
        logSystemInfo();
        logTaieInfo();
    }

    /**
     * Logs environment information based on predefined system properties.
     */
    private static void logEnvInfo() {
        for (String key : ENV_KEYS) {
            String value = System.getProperty(key);
            logger.info("{}: {}", key, value);
        }
    }

    /**
     * Logs system information such as the number of CPU cores and total memory size.
     */
    private static void logSystemInfo() {
        var osBean = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        int cpuCores = osBean.getAvailableProcessors();
        long memoryMB = osBean.getTotalMemorySize() / (1024 * 1024);
        logger.info("Available CPU cores: {}", cpuCores);
        logger.info("Total memory: {}MB", memoryMB);
    }

    /**
     * Logs Tai-e version and commit information by attempting to read from the build properties file
     * or fallback methods if the build properties are not available.
     */
    private static void logTaieInfo() {
        Properties properties = getBuildProperties();
        String version = properties != null
                ? properties.getProperty(VERSION_KEY)
                : UNKNOWN;
        logger.info("Tai-e Version: {}", version);
        String commit = properties != null
                ? properties.getProperty(COMMIT_KEY)
                : UNKNOWN;
        logger.info("Tai-e Commit: {}", commit);
    }

    /**
     * Retrieves the build properties of the current JAR file, if available.
     *
     * @return the build properties, or {@code null} if an error occurs or the build properties is not found
     */
    @Nullable
    private static Properties getBuildProperties() {
        try {
            URL url = RuntimeInfoLogger.class.getProtectionDomain().getCodeSource().getLocation();
            if (url.getPath().endsWith(".jar")) {
                var jarConnection = (JarURLConnection) new URL("jar:" + url + "!/")
                        .openConnection();
                JarFile jarFile = jarConnection.getJarFile();
                var buildPropsEntry = jarFile.getJarEntry(BUILD_PROPERTY_FILE_PATH);
                if (buildPropsEntry != null) {
                    try (var inputStream = jarFile.getInputStream(buildPropsEntry)) {
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        return properties;
                    }
                }
            } else {
                try (var inputStream = RuntimeInfoLogger.class
                        .getClassLoader().getResourceAsStream(BUILD_PROPERTY_FILE_PATH)) {
                    if (inputStream != null) {
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        return properties;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read build properties: {}", e.toString());
        }
        return null;
    }

}
