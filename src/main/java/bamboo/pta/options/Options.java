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

    // ---------- pre-processing options ----------
    @Option(names = "--pre-build-ir",
            description = "Build Bamboo IR for all available methods before" +
                    " starting pointer analysis (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean preBuildIR;

    // ---------- pointer analysis options ----------
    @Option(names = "-jdk",
            description = "JDK version of the standard library being analyzed" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "0")
    private int jdkVersion;

    @Option(names = "--no-implicit-entries",
            description = "Analyze implicit reachable entry methods" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean implicitEntries;

    @Option(names = "--no-native-model",
            description = "Enable native model (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean nativeModel;

    @Option(names = {"-cs", "--context-sensitivity"},
            description = "Context sensitivity for pointer analysis" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "ci")
    private String contextSensitivity;

    @Option(names = "--merge-string-constants",
            description = "Merge string constants (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean mergeStringConstants;

    @Option(names = "--no-merge-string-objects",
            description = "Merge string objects (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean mergeStringObjects;

    @Option(names = "--no-merge-string-builders",
            description = "Merge string builders and buffers" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean mergeStringBuilders;

    // ---------- debugging options ----------
    @Option(names = "--dump-classes",
            description = "Dump classes", defaultValue = "false")
    private boolean dumpClasses;

    @Option(names = "--verbose",
            description = "Output analysis details", defaultValue = "false")
    private boolean verbose;

    @Option(names = "--test-mode",
            description = "Flag test mode", defaultValue = "false")
    private boolean testMode;

    @Option(names = {"-o", "--output-results"},
            description = "Output pointer analysis results",
            defaultValue = "false")
    private boolean outputResults;

    // ---------- Soot options ----------
    @Parameters(description = "Arguments for Soot")
    private String[] sootArgs = new String[0];

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

    public boolean isPreBuildIR() {
        return preBuildIR;
    }

    public int jdkVersion() {
        return jdkVersion;
    }

    public boolean analyzeImplicitEntries() {
        return implicitEntries;
    }

    public boolean enableNativeModel() {
        return nativeModel;
    }

    public String getContextSensitivity() {
        return contextSensitivity;
    }

    public boolean isMergeStringConstants() {
        return mergeStringConstants;
    }

    public boolean isMergeStringObjects() {
        return mergeStringObjects;
    }

    public boolean isMergeStringBuilders() {
        return mergeStringBuilders;
    }

    public boolean isDumpClasses() {
        return dumpClasses;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public boolean isOutputResults() {
        return outputResults;
    }

    public String[] getSootArgs() {
        return sootArgs;
    }
}
