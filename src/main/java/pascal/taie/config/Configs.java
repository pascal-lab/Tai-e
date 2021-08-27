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

package pascal.taie.config;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * Static utility methods for config system.
 */
public final class Configs {

    private Configs() {
    }

    /**
     * Default directory for Tai-e's output.
     */
    private final static File outputDir = new File("output");

    static {
        if (!outputDir.exists()) {
            // Ensure the existence of output directory.
            outputDir.mkdirs();
        }
    }

    public static File getOutputDir() {
        return outputDir;
    }

    /**
     * File name of analysis configuration.
     * TODO: the path of configuration file is hardcoded, make it configurable?
     */
    private static final String CONFIG = "tai-e-analyses.yml";

    /**
     * @return the content of analysis configuration.
     */
    public static InputStream getAnalysisConfig() {
        return Configs.class
                .getClassLoader()
                .getResourceAsStream(CONFIG);
    }

    /**
     * @return the URL of analysis configuration.
     */
    public static URL getAnalysisConfigURL() {
        return Configs.class
                .getClassLoader()
                .getResource(CONFIG);
    }

    /**
     * @return default file for outputting options.
     */
    static File getDefaultOptions() {
        return new File(outputDir, "options.yml");
    }

    /**
     * @return default file for outputting analysis plan.
     */
    public static File getDefaultPlan() {
        return new File(outputDir, "tai-e-plan.yml");
    }

    /**
     * Extracts analysis id from given require item.
     */
    static String extractId(String require) {
        int index = require.indexOf('(');
        return index == -1 ? require :
                require.substring(0, index);
    }

    /**
     * Extracts conditions (represented by a string) from given require item.
     */
    static String extractConditions(String require) {
        int index = require.indexOf('(');
        return index == -1 ? null :
                require.substring(index + 1, require.length() - 1);
    }

    /**
     * Checks if options satisfy the given conditions.
     * Examples of conditions:
     * a=b
     * a=b&x=y
     * a=b|c|d&x=y
     * TODO: comprehensive error handling for invalid conditions
     */
    static boolean satisfyConditions(String conditions, AnalysisOptions options) {
        if (conditions != null) {
            outer:
            for (String conds : conditions.split("&")) {
                String[] splits = conds.split("=");
                String key = splits[0];
                String value = splits[1];
                if (value.contains("|")) { // a=b|c
                    // Check each individual value, if one match,
                    // then this condition can be satisfied.
                    for (String v : value.split("\\|")) {
                        if (options.get(key).toString().equals(v)) {
                            continue outer;
                        }
                    }
                    return false;
                } else if (!Objects.toString(options.get(key)).equals(value)) { // a=b
                    return false;
                }
            }
        }
        return true;
    }
}
