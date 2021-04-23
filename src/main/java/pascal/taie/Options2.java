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

import java.util.Collections;
import java.util.Map;

@Command(name = "Options",
        description = "Tai-e options",
        version = "0.1")
public class Options2 {

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

    @Option(names = "--no-native-model",
            description = "Enable native model (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean nativeModel;

    // ---------- specific analysis options ----------
    @Option(names = {"-a", "--analysis"},
            description = "Analyses to be performed", split = ";")
    private Map<String, String> analyses = Collections.emptyMap();
    
    // ---------- debugging options ----------
    @Option(names = "--dump-classes",
            description = "Dump classes", defaultValue = "false")
    private boolean dumpClasses;

    @Option(names = "--test-mode",
            description = "Flag test mode", defaultValue = "false")
    private boolean testMode;

    /**
     * Parse arguments and return new Options object.
     */
    public static Options2 parse(String... args) {
        Options2 options = CommandLine.populateCommand(new Options2(), args);
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

    public boolean enableNativeModel() {
        return nativeModel;
    }

    public Map<String, String> getAnalyses() {
        return analyses;
    }

    public boolean isDumpClasses() {
        return dumpClasses;
    }

    public boolean isTestMode() {
        return testMode;
    }
}
