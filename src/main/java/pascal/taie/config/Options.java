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

package pascal.taie.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.WorldBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Option class for Tai-e.
 * We name this class in the plural to avoid name collision with {@link Option}.
 */
@Command(name = "Options",
        description = "Tai-e options",
        usageHelpWidth = 120
)
public class Options implements Serializable {

    private static final Logger logger = LogManager.getLogger(Options.class);

    private static final String OPTIONS_FILE = "options.yml";

    private static final String DEFAULT_OUTPUT_DIR = "output";

    // ---------- file-based options ----------
    @JsonProperty
    @Option(names = "--options-file",
            description = "The options file")
    private File optionsFile;

    // ---------- information options ----------
    @JsonProperty
    @Option(names = {"-h", "--help"},
            description = "Display this help message",
            defaultValue = "false",
            usageHelp = true)
    private boolean printHelp;

    public boolean isPrintHelp() {
        return printHelp;
    }

    public void printHelp() {
        CommandLine cmd = new CommandLine(this);
        cmd.setUsageHelpLongOptionsMaxWidth(30);
        cmd.usage(System.out);
    }

    // ---------- program options ----------
    @JsonProperty
    @JsonSerialize(contentUsing = FilePathSerializer.class)
    @Option(names = {"-cp", "--class-path"},
            description = "Class path. This option can be repeated"
                    + " multiple times to specify multiple paths.",
            converter = ClassPathConverter.class)
    private List<String> classPath = List.of();

    public List<String> getClassPath() {
        return classPath;
    }

    @JsonProperty
    @JsonSerialize(contentUsing = FilePathSerializer.class)
    @Option(names = {"-acp", "--app-class-path"},
            description = "Application class path. This option can be repeated"
                    + " multiple times to specify multiple paths.",
            converter = ClassPathConverter.class)
    private List<String> appClassPath = List.of();

    public List<String> getAppClassPath() {
        return appClassPath;
    }

    @JsonProperty
    @Option(names = {"-m", "--main-class"}, description = "Main class")
    private String mainClass;

    public String getMainClass() {
        return mainClass;
    }

    @JsonProperty
    @Option(names = {"--input-classes"},
            description = "The classes should be included in the World of analyzed program." +
                    " You can specify class names or paths to input files (.txt)." +
                    " Multiple entries are split by ','",
            split = ",",
            paramLabel = "<inputClass|inputFile>")
    private List<String> inputClasses = List.of();

    public List<String> getInputClasses() {
        return inputClasses;
    }

    @JsonProperty
    @Option(names = "-java",
            description = "Java version used by the program being analyzed" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "6")
    private int javaVersion;

    public int getJavaVersion() {
        return javaVersion;
    }

    @JsonProperty
    @Option(names = {"-pp", "--prepend-JVM"},
            description = "Prepend class path of current JVM to Tai-e's class path" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean prependJVM;

    public boolean isPrependJVM() {
        return prependJVM;
    }

    @JsonProperty
    @Option(names = {"-ap", "--allow-phantom"},
            description = "Allow Tai-e to process phantom references, i.e.," +
                    " the referenced classes that are not found in the class paths" +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean allowPhantom;

    public boolean isAllowPhantom() {
        return allowPhantom;
    }

    // ---------- general analysis options ----------
    @JsonProperty
    @Option(names = "--world-builder",
            description = "Specify world builder class (default: ${DEFAULT-VALUE})",
            defaultValue = "pascal.taie.frontend.soot.SootWorldBuilder")
    private Class<? extends WorldBuilder> worldBuilderClass;

    public Class<? extends WorldBuilder> getWorldBuilderClass() {
        return worldBuilderClass;
    }

    @JsonProperty
    @JsonSerialize(using = OutputDirSerializer.class)
    @JsonDeserialize(using = OutputDirDeserializer.class)
    @Option(names = "--output-dir",
            description = "Specify output directory (default: ${DEFAULT-VALUE})"
                    + ", '" + PlaceholderAwareFile.AUTO_GEN + "' can be used as a placeholder"
                    + " for an automatically generated timestamp",
            defaultValue = DEFAULT_OUTPUT_DIR,
            converter = OutputDirConverter.class)
    private File outputDir;

    public File getOutputDir() {
        return outputDir;
    }

    @JsonProperty
    @Option(names = "--pre-build-ir",
            description = "Build IR for all available methods before" +
                    " starting any analysis (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean preBuildIR;

    public boolean isPreBuildIR() {
        return preBuildIR;
    }

    @JsonProperty
    @Option(names = {"-wc", "--world-cache-mode"},
            description = "Enable world cache mode to save build time"
                    + " by caching the completed built world to the disk.",
            defaultValue = "false")
    private boolean worldCacheMode;

    public boolean isWorldCacheMode() {
        return worldCacheMode;
    }

    @JsonProperty
    @Option(names = "-scope",
            description = "Scope for method/class analyses (default: ${DEFAULT-VALUE}," +
                    " valid values: ${COMPLETION-CANDIDATES})",
            defaultValue = "APP")
    private Scope scope;

    public Scope getScope() {
        return scope;
    }

    @JsonProperty
    @Option(names = "--no-native-model",
            description = "Enable native model (default: ${DEFAULT-VALUE})",
            defaultValue = "true",
            negatable = true)
    private boolean nativeModel;

    public boolean enableNativeModel() {
        return nativeModel;
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
            description = "Analyses to be executed",
            paramLabel = "<analysisID[=<options>]>",
            mapFallbackValue = "")
    private Map<String, String> analyses = Map.of();

    public Map<String, String> getAnalyses() {
        return analyses;
    }

    @JsonProperty
    @Option(names = {"-g", "--gen-plan-file"},
            description = "Merely generate analysis plan",
            defaultValue = "false")
    private boolean onlyGenPlan;

    public boolean isOnlyGenPlan() {
        return onlyGenPlan;
    }

    @JsonProperty
    @Option(names = {"-kr", "--keep-result"},
            description = "The analyses whose results are kept" +
                    " (multiple analyses are split by ',', default: ${DEFAULT-VALUE})",
            split = ",", paramLabel = "<analysisID>",
            defaultValue = Plan.KEEP_ALL)
    private Set<String> keepResult;

    public Set<String> getKeepResult() {
        return keepResult;
    }

    /**
     * Parses arguments and return the parsed and post-processed Options.
     */
    public static Options parse(String... args) {
        Options options = CommandLine.populateCommand(new Options(), args);
        return postProcess(options);
    }

    /**
     * Validates input options and do some post-process on it.
     *
     * @return the Options object after post-process.
     */
    private static Options postProcess(Options options) {
        if (options.optionsFile != null) {
            // If options file is given, we ignore other options,
            // and instead read options from the file.
            options = readRawOptions(options.optionsFile);
        }
        if (options.prependJVM) {
            options.javaVersion = getCurrentJavaVersion();
        }
        if (!options.analyses.isEmpty() && options.planFile != null) {
            // The user should choose either options or plan file to
            // specify analyses to be executed.
            throw new ConfigException("Conflict options: " +
                    "--analysis and --plan-file should not be used simultaneously");
        }
        if (options.getClassPath() != null
                && options.mainClass == null
                && options.inputClasses.isEmpty()
                && options.getAppClassPath() == null) {
            throw new ConfigException("Missing options: " +
                    "at least one of --main-class, --input-classes " +
                    "or --app-class-path should be specified");
        }
        // mkdir for output dir
        if (!options.outputDir.exists()) {
            options.outputDir.mkdirs();
        }
        logger.info("Output directory: {}",
                options.outputDir.getAbsolutePath());
        // TODO: turn off output in testing?
        if (options.optionsFile == null) {
            // write options to file only when it is not given
            writeOptions(options, new File(options.outputDir, OPTIONS_FILE));
        }
        return options;
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
            logger.info("Writing options to {}", output.getAbsolutePath());
            mapper.writeValue(output, options);
        } catch (IOException e) {
            throw new ConfigException("Failed to write options to "
                    + output.getAbsolutePath(), e);
        }
    }

    /**
     * Represents a file that supports placeholder and automatically replaces it
     * with current timestamp values. This class extends the standard File class.
     */
    private static class PlaceholderAwareFile extends File {

        /**
         * The placeholder for an automatically generated timestamp.
         */
        private static final String AUTO_GEN = "$AUTO-GEN";

        private final String rawPathname;

        public PlaceholderAwareFile(String pathname) {
            super(resolvePathname(pathname));
            this.rawPathname = pathname;
        }

        public String getRawPathname() {
            return rawPathname;
        }

        private static String resolvePathname(String pathname) {
            if (pathname.contains(AUTO_GEN)) {
                String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now());
                pathname = pathname.replace(AUTO_GEN, timestamp);
                // check if the output dir already exists
                File file = Path.of(pathname).toAbsolutePath().normalize().toFile();
                if (file.exists()) {
                    throw new RuntimeException("The generated file already exists, "
                            + "please wait for a second to start again: " + pathname);
                }
            }
            return Path.of(pathname).toAbsolutePath().normalize().toString();
        }

    }

    /**
     * @see #outputDir
     */
    private static class OutputDirConverter implements CommandLine.ITypeConverter<File> {
        @Override
        public File convert(String outputDir) {
            return new PlaceholderAwareFile(outputDir);
        }
    }

    /**
     * Serializer for raw {@link #outputDir}.
     */
    private static class OutputDirSerializer extends JsonSerializer<File> {
        @Override
        public void serialize(File value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            if (value instanceof PlaceholderAwareFile file) {
                gen.writeString(toSerializedFilePath(file.getRawPathname()));
            } else {
                throw new RuntimeException("Unexpected type: " + value);
            }
        }
    }

    /**
     * Deserializer for {@link #outputDir}.
     */
    private static class OutputDirDeserializer extends JsonDeserializer<File> {

        @Override
        public File deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            return new PlaceholderAwareFile(p.getValueAsString());
        }
    }

    /**
     * Converter for classpath with system path separator.
     */
    private static class ClassPathConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(File.pathSeparator))
                    .map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .toList();
        }
    }

    /**
     * Serializer for file path. Ensures a path is serialized as a relative path
     * from the working directory rather than an absolute path, thus
     * preserving the portability of the dumped options file.
     */
    private static class FilePathSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeString(toSerializedFilePath(value));
        }
    }

    /**
     * Convert a file to a relative path using the "/" (forward slash)
     * from the working directory, thus preserving the portability of
     * the dumped options file.
     *
     * @param file the file to be processed
     * @return a relative path from the working directory
     */
    private static String toSerializedFilePath(String file) {
        Path workingDir = Path.of("").toAbsolutePath();
        Path path = Path.of(file).toAbsolutePath().normalize();
        return workingDir.relativize(path).toString()
                .replace('\\', '/');
    }

    @Override
    public String toString() {
        return "Options{" +
                "optionsFile=" + optionsFile +
                ", printHelp=" + printHelp +
                ", classPath='" + classPath + '\'' +
                ", appClassPath='" + appClassPath + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", inputClasses=" + inputClasses +
                ", javaVersion=" + javaVersion +
                ", prependJVM=" + prependJVM +
                ", allowPhantom=" + allowPhantom +
                ", worldBuilderClass=" + worldBuilderClass +
                ", outputDir='" + outputDir + '\'' +
                ", preBuildIR=" + preBuildIR +
                ", worldCacheMode=" + worldCacheMode +
                ", scope=" + scope +
                ", nativeModel=" + nativeModel +
                ", planFile=" + planFile +
                ", analyses=" + analyses +
                ", onlyGenPlan=" + onlyGenPlan +
                ", keepResult=" + keepResult +
                '}';
    }
}
