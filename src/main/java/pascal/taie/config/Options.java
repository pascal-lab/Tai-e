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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.WorldBuilder;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.reflection.LogItem;
import pascal.taie.android.util.AndroidJavaVersionInfer;
import pascal.taie.frontend.soot.SootWorldBuilder;
import pascal.taie.language.classes.StringReps;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
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

    private static final Logger logger = LoggerFactory.getLogger(Options.class);

    private static final String OPTIONS_FILE = "options.yml";

    private static final String DEFAULT_OUTPUT_DIR = "output";

    private static final String DEFAULT_WORLD_BUILDER_CLASS =
            "pascal.taie.frontend.java.JavaWorldBuilder";

    private static final String LEGACY_WORLD_BUILDER_CLASS =
            "pascal.taie.frontend.soot.SootWorldBuilder";

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
            description = "Java version used by the program being analyzed")
    private Integer javaVersion;

    public int getJavaVersion() {
        if (javaVersion == null) {
            throw new ConfigException("javaVersion config has not been resolved");
        }
        return javaVersion;
    }

    /**
     * Whether Tai-e uses the current JRE, i.e., the one Tai-e runs on,
     * as the accompanying JRE library for the analyzed program. Using
     * the current JRE is Tai-e's default behavior.
     * <p>
     * This field is intentionally declared as a boxed {@link Boolean}
     * and left uninitialized (i.e., {@code null}) on purpose, so that
     * {@link #resolveJREOptions} can distinguish whether the value has
     * been explicitly specified via the options file from the case where
     * it is absent and should fall back to the default.
     */
    @JsonProperty
    private Boolean useCurrentJRE;

    /**
     * @return whether Tai-e uses the current JRE, i.e., the one Tai-e runs on,
     * as the JRE for the analyzed program.
     */
    public boolean isUseCurrentJRE() {
        return Boolean.TRUE.equals(useCurrentJRE);
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Option(names = {"-pp", "--prepend-JVM"},
            description = "Deprecated compatibility option. Its behavior is " +
                    "now the default when -java and --jre-dir are omitted.",
            defaultValue = "false",
            hidden = true)
    private boolean prependJVM;

    /**
     * @return whether deprecated option {@code -pp}/{@code --prepend-JVM} is specified.
     * @deprecated use {@link #isUseCurrentJRE()} instead.
     */
    @Deprecated
    public boolean isPrependJVM() {
        return prependJVM;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Option(names = {"-ap", "--allow-phantom"},
            description = "Deprecated compatibility option. Phantom classes " +
                    "are now allowed by default and reported with warnings.",
            defaultValue = "false",
            hidden = true)
    private boolean allowPhantom;

    /**
     * @return true, as Tai-e now allows phantom classes by default.
     * @deprecated phantom classes are always allowed.
     */
    @Deprecated
    public boolean isAllowPhantom() {
        return true;
    }

    @JsonProperty
    @Option(names = {"-am", "--android-mode"},
            description = "Enable Android mode (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean androidMode;

    public boolean isAndroidMode() {
        return androidMode;
    }

    @JsonProperty
    @Option(names = {"-ajs", "--android-jars"},
            description = "Specifies the path to Android platforms required for analysis." +
                    " This path is used to locate the necessary Android JAR files for analysis purposes." +
                    " (default: ${DEFAULT-VALUE})",
            defaultValue = "android-benchmarks/android-platforms")
    private String androidJars;

    public String getAndroidJars() {
        return androidJars;
    }

    // ---------- general analysis options ----------
    @JsonProperty
    @Option(names = "--world-builder",
            description = "Specify world builder class (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_WORLD_BUILDER_CLASS)
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
                    + " by caching the completed built world to the disk."
                    + " When enabled, the '--pre-build-ir' option will be"
                    + " enabled automatically. (default: ${DEFAULT-VALUE})",
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
    private Map<String, AnalysisOptions> analyses = Map.of();

    public Map<String, AnalysisOptions> getAnalyses() {
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

    @JsonProperty
    @Option(names = {"--jre-dir"},
            description = "Specify a JRE directory to be used for analysis. " +
                    "When this option is specified, -java must also be specified. " +
                    "The directory accepts one of the following: " +
                    "(1) a JAVA_HOME like dir, which contains lib/ dir or jre/lib/ dir" +
                    "(2) a dir contain rt.jar and related files (Java 1.6-8)" +
                    "(3) a dir contain jrt-fs.jar and modules (JIMAGE) file (Java 9+)")
    private String jreDir;

    public String getJreDir() {
        return jreDir;
    }

    @JsonProperty
    @Option(names = {"--ssa"},
            description = "Enable SSA (Static Single Assignment) Generation for frontend")
    private boolean ssa;

    public boolean isSSA() {
        return ssa;
    }

    /**
     * Parses arguments and return the parsed and post-processed Options.
     */
    public static Options parse(String... args) {
        Options options = new Options();
        CommandLine cmd = new CommandLine(options);
        cmd.registerConverter(AnalysisOptions.class, new AnalysisOptionsConverter())
                .parseArgs(args);
        return postProcess(options);
    }

    /**
     * Validates input options and do some post-process on it.
     *
     * @return the Options object after post-process.
     */
    private static Options postProcess(Options options) {
        // TODO: Refactor this logic: loading already-processed options,
        //  reading options, and modifying options are mixed together now.
        if (options.optionsFile != null) {
            // If options file is given, we ignore other options,
            // and instead read options from the file.
            options = readRawOptions(options.optionsFile);
        }
        resolveJREOptions(options);
        if (options.allowPhantom) {
            logger.warn("DEPRECATED OPTION: Please stop using '-ap/--allow-phantom'. "
                    + "This option will be removed in a future version; allowing "
                    + "phantom classes is now the default.");
        }
        if (options.worldBuilderClass != null
                && options.worldBuilderClass.getName().equals(LEGACY_WORLD_BUILDER_CLASS)) {
            logger.warn("DEPRECATED OPTION: Please stop using the legacy frontend "
                            + "selected by '--world-builder'. "
                            + "This legacy frontend will be removed in a future version; "
                            + "the new Java frontend is now the default. Please omit "
                            + "'--world-builder' or use '--world-builder {}' instead.",
                    DEFAULT_WORLD_BUILDER_CLASS);
        }
        if (options.worldCacheMode) {
            options.preBuildIR = true;
        }
        if (!options.analyses.isEmpty() && options.planFile != null) {
            // The user should choose either options or plan file to
            // specify analyses to be executed.
            throw new ConfigException("Conflict options: " +
                    "--analysis and --plan-file should not be used simultaneously");
        }
        if (options.androidMode) { // analyze Android program
            if (options.getClassPath().isEmpty()) {
                throw new ConfigException("Missing options: apk is missing" +
                        " (should be specified by -cp)");
            }

            // Android analysis still uses the Soot frontend.
            options.worldBuilderClass = SootWorldBuilder.class;

            // infer Java version from Android target SDK
            String apkPath = options.classPath.get(0);
            options.javaVersion = AndroidJavaVersionInfer.inferFromApk(apkPath);
        } else { // analyze Java program
            if (options.getClassPath().isEmpty()
                    && options.mainClass == null
                    && options.inputClasses.isEmpty()
                    && options.getAppClassPath() == null) {
                throw new ConfigException("Missing options: " +
                        "at least one of --main-class, --input-classes " +
                        "or --app-class-path should be specified");
            }
        }

        if (options.analyses.containsKey(PointerAnalysis.ID)
                && options.analyses.get(PointerAnalysis.ID).has("reflection-log")) {
            options.addReflectionLogClasses();
        }
        // mkdir for output dir
        if (!options.outputDir.exists()) {
            options.outputDir.mkdirs();
        }
        logger.info("Output directory: {}",
                options.outputDir.getAbsolutePath());
        // write options to file for future reviewing and issue submitting
        writeOptions(options, new File(options.outputDir, OPTIONS_FILE));
        return options;
    }

    /**
     * Resolves JRE selection options.
     */
    private static void resolveJREOptions(Options options) {
        // -pp is a deprecated compatibility option.
        // It forces Tai-e to use the current JRE.
        if (options.prependJVM) {
            logger.warn("DEPRECATED OPTION: Please stop using '-pp/--prepend-JVM'. "
                    + "This option will be removed in a future version; its behavior "
                    + "is now the default when -java and --jre-dir are omitted. Tai-e "
                    + "will use the current Java runtime for compatibility.");
            if (options.javaVersion != null
                    || options.jreDir != null
                    || Boolean.FALSE.equals(options.useCurrentJRE)) {
                throw new ConfigException("Conflict options: "
                        + "-pp/--prepend-JVM forces the current JRE, so it "
                        + "cannot be used with -java/--jre-dir/useCurrentJRE=false");
            }
            options.useCurrentJRE = true;
            options.javaVersion = getCurrentJavaVersion();
        } else if (options.useCurrentJRE != null) {
            // useCurrentJRE is explicitly specified, so it is considered as already
            // resolved; check the consistency with related options.
            if (options.javaVersion == null
                    || (options.useCurrentJRE && options.jreDir != null)) {
                throw new ConfigException("Invalid options file: "
                        + "useCurrentJRE must be paired with javaVersion, and "
                        + "useCurrentJRE=true cannot be paired with --jre-dir");
            }
        } else if (options.javaVersion != null) {
            // -java selects a JRE version. If --jre-dir is also specified, that
            // version is loaded from the user-provided directory; otherwise, it
            // is loaded from java-benchmarks later.
            options.useCurrentJRE = false;
        } else {
            // --jre-dir only changes where the selected JRE is loaded from, so
            // it cannot be used without -java.
            if (options.jreDir != null) {
                throw new ConfigException("Missing option: "
                        + "-java must be specified when --jre-dir is used");
            }

            // no Java version is specified: use the current JRE by default.
            options.useCurrentJRE = true;
            options.javaVersion = getCurrentJavaVersion();
        }
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
                        .disable(YAMLGenerator.Feature.SPLIT_LINES)
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
     * the dumped options file as much as possible.
     *
     * @param file the file to be processed
     * @return the relative path from the working directory;
     * if the file cannot be relativized, the absolute path is returned.
     */
    private static String toSerializedFilePath(String file) {
        Path workingDir = Path.of("").toAbsolutePath().normalize();
        Path filePath = Path.of(file).toAbsolutePath().normalize();
        try {
            filePath = workingDir.relativize(filePath);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to get relative path of {}," +
                    " use its absolute path in options file", file);
        }
        return filePath.toString().replace('\\', '/');
    }

    private static class AnalysisOptionsConverter implements CommandLine.ITypeConverter<AnalysisOptions> {
        @Override
        public AnalysisOptions convert(String value) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JavaType mapType = mapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class);
            String optStr = toYAMLString(value);
            try {
                Map<String, Object> optsMap = optStr.isBlank()
                        ? Map.of()
                        // Leverage Jackson to parse YAML string to Map
                        : mapper.readValue(optStr, mapType);
                return new AnalysisOptions(optsMap);
            } catch (JsonProcessingException e) {
                throw new ConfigException("Invalid analysis options: " + value, e);
            }
        }

        /**
         * Converts option string to a valid YAML string.
         * The option string is of format "key1:value1;key2:value2;...".
         */
        private static String toYAMLString(String optValue) {
            StringJoiner joiner = new StringJoiner("\n");
            for (String keyValue : optValue.split(";")) {
                if (!keyValue.isBlank()) {
                    int i = keyValue.indexOf(':'); // split keyValue
                    if (i == -1) {
                        throw new IllegalArgumentException("Invalid argument format '" + keyValue
                                + "'. Expected format: 'key:value'");
                    }
                    joiner.add(keyValue.substring(0, i) + ": "
                            + keyValue.substring(i + 1));
                }
            }
            return joiner.toString();
        }
    }

    /**
     * Add classes in reflection log to the input classes.
     * <p>
     * TODO: this is still a tentative solution.
     */
    private void addReflectionLogClasses() {
        List<String> inputClasses = new ArrayList<>(this.inputClasses);
        String path = analyses.get(PointerAnalysis.ID).getString("reflection-log");
        if (path != null) {
            Set<String> primitiveTypeNames = Set.of(
                    "boolean", "byte", "char", "short", "int", "long",
                    "float", "double", "void");
            LogItem.load(path).forEach(item -> {
                // add target class
                String target = item.target;
                String targetClass;
                if (target.startsWith("<")) {
                    targetClass = StringReps.getClassNameOf(target);
                } else {
                    targetClass = target;
                }
                if (StringReps.isArrayType(targetClass)) {
                    targetClass = StringReps.getBaseTypeNameOf(targetClass);
                }
                if (!primitiveTypeNames.contains(targetClass)) {
                    inputClasses.add(targetClass);
                }
            });
        }
        this.inputClasses = List.copyOf(inputClasses);
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
                ", androidJars=" + androidJars +
                ", javaVersion=" + javaVersion +
                ", androidMode=" + androidMode +
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
