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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.WorldBuilder;
import pascal.taie.frontend.soot.SootWorldBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Command(name = "Options",
        description = "Tai-e options",
        version = "0.1")
public class Options {

    private static final Logger logger = LogManager.getLogger(Options.class);

    // ---------- file-based options ----------
    @Option(names = "--options-file",
            description = "The options file.")
    private File optionsFile;

    public File getOptionsFile() {
        return optionsFile;
    }

    // ---------- information options ----------
    @Option(names = {"-v", "--version"},
            description = "Display version information",
            defaultValue = "false", versionHelp = true)
    private boolean printVersion = false;

    public boolean isPrintVersion() {
        return printVersion;
    }

    public void printVersion() {
        new CommandLine(this).printVersionHelp(System.out);
    }

    @JsonProperty
    @Option(names = {"-h", "--help"},
            description = "Display this help message",
            defaultValue = "false", usageHelp = true)
    private boolean printHelp = false;

    public boolean isPrintHelp() {
        return printHelp;
    }

    public void printHelp() {
        new CommandLine(this).usage(System.out);
    }

    // ---------- program options ----------
    @JsonProperty
    @Option(names = "-java",
            description = "Java version used by the program being analyzed" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "6")
    private int javaVersion = 6;

    public int getJavaVersion() {
        return javaVersion;
    }

    @JsonProperty
    @Option(names = {"-pp", "--prepend-JVM"},
            description = "Prepend class path of current JVM to Tai-e's class path" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean prependJVM = false;

    public boolean isPrependJVM() {
        return prependJVM;
    }

    @JsonProperty
    @Option(names = {"-cp", "--class-path"},
            description = "Class path")
    private String classPath;

    public String getClassPath() {
        return classPath;
    }

    @JsonProperty
    @Option(names = {"-m", "--main-class"},
            description = "Main class")
    private String mainClass;

    public String getMainClass() {
        return mainClass;
    }

    // ---------- general analysis options ----------
    @JsonProperty
    @Option(names = "--world-builder",
            description = "Specify world builder class (default: ${DEFAULT-VALUE})",
            defaultValue = "pascal.taie.frontend.soot.SootWorldBuilder")
    private Class<? extends WorldBuilder> worldBuilderClass = SootWorldBuilder.class;

    public Class<? extends WorldBuilder> getWorldBuilderClass() {
        return worldBuilderClass;
    }

    @JsonProperty
    @Option(names = "--pre-build-ir",
            description = "Build Tai-e IR for all available methods before" +
                    " starting pointer analysis (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean preBuildIR = false;

    public boolean isPreBuildIR() {
        return preBuildIR;
    }

    @JsonProperty
    @Option(names = "--no-native-model",
            description = "Enable native model (default: ${DEFAULT-VALUE})",
            defaultValue = "true", negatable = true)
    private boolean nativeModel = true;

    public boolean enableNativeModel() {
        return nativeModel;
    }

    @JsonProperty
    @Option(names = "--dump-classes",
            description = "Dump classes", defaultValue = "false")
    private boolean dumpClasses = false;

    public boolean isDumpClasses() {
        return dumpClasses;
    }

    // ---------- specific analysis options ----------
    @JsonProperty
    @Option(names = {"-p", "--plan-file"},
            description = "The analysis plan file")
    private File planFile;

    public File getPlanFile() {
        return planFile;
    }

    @JsonProperty
    @Option(names = {"-a", "--analysis"},
            description = "Analyses to be executed", split = ";",
            mapFallbackValue = "")
    private Map<String, String> analyses = Map.of();

    public Map<String, String> getAnalyses() {
        return analyses;
    }

    @JsonProperty
    @Option(names = {"-g", "--gen-plan-file"},
            description = "Merely generate analysis plan",
            defaultValue = "false")
    private boolean onlyGenPlan = false;

    public boolean isOnlyGenPlan() {
        return onlyGenPlan;
    }

    // ---------- debugging options ----------
    @Option(names = "--test-mode",
            description = "Flag test mode", defaultValue = "false")
    private boolean testMode;

    public boolean isTestMode() {
        return testMode;
    }

    /**
     * Parses arguments and return the parsed and post-processed Options.
     */
    public static Options parse(String... args) {
        Options options = CommandLine.populateCommand(new Options(), args);
        return options.postProcess();
    }

    /**
     * Validates input options and do some post-process on it.
     * @return the Options object after post-process.
     */
    private Options postProcess() {
        Options result = optionsFile == null ? this :
                // If options file is given, we ignore other options,
                // and instead read options from the file.
                readRawOptions(optionsFile);
        if (isPrependJVM()) {
            javaVersion = getCurrentJavaVersion();
        }
        if (!analyses.isEmpty() && planFile != null) {
            // The user should choose either options or plan file to
            // specify analyses to be executed.
            throw new ConfigException("Conflict options: " +
                    "--analysis and --plan-file should not be used simultaneously");
        }
        // TODO: turn off output in test mode?
        writeOptions(result, ConfigUtils.getDefaultOptions());
        return result;
    }

    /**
     * Reads options from file.
     * Note: the returned options have not been post-processed.
     */
    private static Options readRawOptions(File file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(file, Options.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read options from " + file, e);
        }
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

    /**
     * Writes options to given file.
     */
    private static void writeOptions(Options options, File output) {
        ObjectMapper mapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            logger.info("Writing options to " + output);
            mapper.writeValue(output, options);
        } catch (IOException e) {
            throw new ConfigException("Failed to write options " + output, e);
        }
    }

    @Override
    public String toString() {
        return "Options{" +
                "version=" + printVersion +
                ", help=" + printHelp +
                ", javaVersion=" + javaVersion +
                ", prependJVM=" + prependJVM +
                ", classPath='" + classPath + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", worldBuilderClass=" + worldBuilderClass +
                ", preBuildIR=" + preBuildIR +
                ", nativeModel=" + nativeModel +
                ", dumpClasses=" + dumpClasses +
                ", planFile='" + planFile + '\'' +
                ", analyses=" + analyses +
                ", genPlanFile=" + onlyGenPlan +
                ", testMode=" + testMode +
                '}';
    }
}
