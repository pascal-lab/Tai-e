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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Bamboo",
        description = "A pointer analysis framework for Java",
        showEndOfOptionsDelimiterInUsageHelp = true,
        version = "0.1")
public class Options {

    // Default options
    private static Options options = CommandLine.populateCommand(new Options());

    // ---------- information options ----------
    @Option(names = {"-V", "--version"},
            description = "Display version information",
            defaultValue = "false", versionHelp = true)
    boolean version;

    @Option(names = {"-h", "--help"},
            description = "Display this help message",
            defaultValue = "false", usageHelp = true)
    boolean help;

    // ---------- pointer analysis options ----------
    @Option(names = "--no-implicit-entries",
            description = "Analyze implicit reachable entry methods",
            defaultValue = "true", negatable = true)
    private boolean implicitEntries;

    @Option(names = {"-cs", "--context-sensitivity"},
            description = "Context sensitivity for pointer analysis",
            defaultValue = "ci")
    private String contextSensitivity;

    // ---------- debugging options ----------
    @Option(names = "--dump-classes",
            description = "Dump classes", defaultValue = "false")
    private boolean dumpClasses;

    @Option(names = "--verbose",
            description = "Output analysis details", defaultValue = "false")
    private boolean verbose;

    // ---------- Soot options ----------
    @Parameters(description = "Arguments for Soot")
    private String[] sootArgs;

    public static Options get() {
        return options;
    }

    public static void set(Options options) {
        Options.options = options;
    }

    /**
     * Parse arguments and set new Options object.
     */
    public static void parse(String... args) {
        options = CommandLine.populateCommand(new Options(), args);
    }

    public void printHelp() {
        new CommandLine(this).usage(System.out);
    }

    public void printVersion() {
        new CommandLine(this).printVersionHelp(System.out);
    }

    public boolean shouldShowHelp() {
        return help;
    }

    public boolean shouldShowVersion() {
        return version;
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

    public boolean isVerbose() {
        return verbose;
    }

    public String[] getSootArgs() {
        return sootArgs;
    }
}
