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

package pascal.taie;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Command(name = "Options",
        description = "Tai-e options",
        version = "0.1")
public class Options {

    // ---------- information options ----------
    @Option(names = {"-v", "--version"},
            description = "Display version information",
            defaultValue = "false", versionHelp = true)
    private boolean version;

    @Option(names = {"-h", "--help"},
            description = "Display this help message",
            defaultValue = "false", usageHelp = true)
    private boolean help;

    // ---------- program options ----------
    @Option(names = "-java",
            description = "Java version used by the program being analyzed" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "6")
    private int javaVersion;

    @Option(names = {"-pp", "--prepend-JVM"},
            description = "Prepend class path of current JVM to Tai-e's class path" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean prependJVM;

    @Option(names = {"-cp", "--class-path"},
            description = "Class path")
    private String classPath;

    @Option(names = {"-m", "--main-class"},
            description = "Main class")
    private String mainClass;

    // ---------- general analysis options ----------
    @Option(names = "--world-builder",
            description = "Specify world builder class (default: ${DEFAULT-VALUE})",
            defaultValue = "pascal.taie.frontend.soot.SootWorldBuilder")
    private Class<? extends WorldBuilder> worldBuilderClass;

    @Option(names = "--pre-build-ir",
            description = "Build Tai-e IR for all available methods before" +
                    " starting pointer analysis (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean preBuildIR;

    @SuppressWarnings("FieldMayBeFinal")
    @Option(names = {"-p", "--passes"},
            description = "Analysis passes to run", split = ",")
    private List<String> passClasses = Collections.emptyList();

    // ---------- pointer analysis options ----------
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

    @Option(names = "--no-merge-exception-objects",
            description = "Merge exception objects by their types" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean mergeExceptionObjects;

    @Option(names = {"-o", "--output-results"},
            description = "Output pointer analysis results",
            defaultValue = "false")
    private boolean outputResults;

    @Option(names = {"-f", "--output-file"}, description = "The output file")
    private File outputFile;

    // ---------- debugging options ----------
    @Option(names = "--dump-classes",
            description = "Dump classes", defaultValue = "false")
    private boolean dumpClasses;

    @Option(names = "--dump-call-graph",
            description = "Dump call graph", defaultValue = "false")
    private boolean dumpCallGraph;

    @Option(names = "--test-mode",
            description = "Flag test mode", defaultValue = "false")
    private boolean testMode;

    /**
     * Parse arguments and return new Options object.
     */
    public static Options parse(String... args) {
        Options options = CommandLine.populateCommand(new Options(), args);
        // post-process options
        if (options.isPrependJVM()) {
            options.javaVersion = getCurrentJavaVersion();
        }
        return options;
    }

    static int getCurrentJavaVersion() {
        String version = System.getProperty("java.version");
        String[] splits = version.split("\\.");
        int i0 = Integer.parseInt(splits[0]);
        if (i0 == 1) { // format 1.x.y_z (for Java 1-8)
            return Integer.parseInt(splits[1]);
        } else { // format x.y.z (for Java 9+)
            return i0;
        }
    }

    public boolean isPrintVersion() {
        return version;
    }

    public void printVersion() {
        new CommandLine(this).printVersionHelp(System.out);
    }

    public boolean isPrintHelp() {
        return help;
    }

    public void printHelp() {
        new CommandLine(this).usage(System.out);
    }

    public int getJavaVersion() {
        return javaVersion;
    }

    public boolean isPrependJVM() {
        return prependJVM;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Class<? extends WorldBuilder> getWorldBuilderClass() {
        return worldBuilderClass;
    }

    public boolean isPreBuildIR() {
        return preBuildIR;
    }

    public List<String> getPassClasses() {
        return passClasses;
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

    public boolean isMergeExceptionObjects() {
        return mergeExceptionObjects;
    }

    public boolean isOutputResults() {
        return outputResults;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean isDumpClasses() {
        return dumpClasses;
    }

    public boolean isDumpCallGraph() {
        return dumpCallGraph;
    }

    public boolean isTestMode() {
        return testMode;
    }
}
