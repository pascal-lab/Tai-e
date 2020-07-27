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

package bamboo.pta.options;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Bamboo",
        description = "A pointer analysis framework for Java",
        mixinStandardHelpOptions = true,
        showEndOfOptionsDelimiterInUsageHelp = true,
        version = "0.1")
public class Options implements Runnable {

    private static Options options;

    public static Options get() {
        return options;
    }

    public static void set(Options options) {
        Options.options = options;
    }

    @Option(names = "--no-implicit-entries",
            description = "Analyze implicit reachable entry methods",
            defaultValue = "true", negatable = true)
    private boolean implicitEntries;

    @Option(names = { "-cs", "--context-sensitivity" },
            description = "Context sensitivity for pointer analysis",
            defaultValue = "ci")
    private String contextSensitivity;

    @Option(names = "--dump-classes",
            description = "Dump classes", defaultValue = "false")
    private boolean dumpClasses;

    // Soot-related options
    @Parameters(description = "Arguments for Soot")
    private String[] sootArgs;

    @Override
    public void run() {
        set(this);
    }

    public boolean analyzeImplicitEntries() {
        return implicitEntries;
    }

    public String getContextSensitivity() {
        return contextSensitivity;
    }

    public boolean isDumpClasses() {
        return dumpClasses;
    }

    public String[] getSootArgs() {
        return sootArgs;
    }
}
