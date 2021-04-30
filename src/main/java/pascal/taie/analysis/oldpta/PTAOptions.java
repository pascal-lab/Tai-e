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

package pascal.taie.analysis.oldpta;

import pascal.taie.config.Options;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;

@Command(name = "PTAOptions",
        description = "Pointer analysis options",
        showEndOfOptionsDelimiterInUsageHelp = true,
        version = "0.1")
public class PTAOptions extends Options {

    // Default options
    private static PTAOptions options = CommandLine.populateCommand(new PTAOptions());

    // ---------- pointer analysis options ----------
    @Option(names = "--no-implicit-entries",
            description = "Analyze implicit reachable entry methods" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean implicitEntries;

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

    @Option(names = "--no-merge-exception-objects",
            description = "Merge exception objects by their types" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean mergeExceptionObjects;

    // ---------- debugging options ----------
    @Option(names = "--dump-call-graph",
            description = "Dump call graph", defaultValue = "false")
    private boolean dumpCallGraph;

    @Option(names = {"-o", "--output-results"},
            description = "Output pointer analysis results",
            defaultValue = "false")
    private boolean outputResults;

    @Option(names = {"-f", "--output-file"}, description = "The output file")
    private File outputFile;

    public void printVersion() {
        new CommandLine(this).printVersionHelp(System.out);
    }

    public void printHelp() {
        new CommandLine(this).usage(System.out);
    }

    public boolean analyzeImplicitEntries() {
        return implicitEntries;
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

    public boolean isMergeExceptionObjects() {
        return mergeExceptionObjects;
    }

    public boolean isDumpCallGraph() {
        return dumpCallGraph;
    }

    public boolean isOutputResults() {
        return outputResults;
    }

    public File getOutputFile() {
        return outputFile;
    }

    // ---------- Soot options ----------
    @SuppressWarnings("FieldMayBeFinal")
    @Parameters(description = "Arguments for Soot")
    private String[] sootArgs = new String[0];

    public static PTAOptions get() {
        return options;
    }

    public static void set(PTAOptions options) {
        PTAOptions.options = options;
    }

    /**
     * Parse arguments and set new Options object.
     */
    public static void parseOptions(String... args) {
        options = CommandLine.populateCommand(new PTAOptions(), args);
    }

    public String[] getSootArgs() {
        return sootArgs;
    }
}
