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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Configuration for an analysis to be executed.
 * <p>
 * Different from {@link AnalysisConfig} which is specified by configuration file,
 * {@link PlanConfig} is specified by either plan file or options.
 *
 * @see AnalysisConfig
 */
public class PlanConfig {

    private static final Logger logger = LogManager.getLogger(PlanConfig.class);

    private static final String PLAN_FILE = "tai-e-plan.yml";

    /**
     * Unique identifier of the analysis.
     */
    @JsonProperty
    private final String id;

    /**
     * Options for the analysis.
     */
    @JsonProperty
    private final AnalysisOptions options;

    @JsonCreator
    public PlanConfig(
            @JsonProperty("id") String id,
            @JsonProperty("options") AnalysisOptions options) {
        this.id = id;
        this.options = Objects.requireNonNullElse(options,
                AnalysisOptions.emptyOptions());
    }

    public String getId() {
        return id;
    }

    public AnalysisOptions getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "PlanConfig{" +
                "id='" + id + '\'' +
                ", options=" + options +
                '}';
    }

    /**
     * Read a list of PlanConfig from given file.
     */
    public static List<PlanConfig> readConfigs(File file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, PlanConfig.class);
        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            throw new ConfigException("Failed to read plan file " + file, e);
        }
    }

    /**
     * Reads a list of PlanConfig from options.
     */
    public static List<PlanConfig> readConfigs(Options options) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType mapType = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        return options.getAnalyses().entrySet()
                .stream()
                .map(entry -> {
                    String id = entry.getKey();
                    String optStr = toYAMLString(entry.getValue());
                    try {
                        Map<String, Object> optsMap = optStr.isBlank()
                                ? Map.of()
                                // Leverage Jackson to parse YAML string to Map
                                : mapper.readValue(optStr, mapType);
                        return new PlanConfig(id, new AnalysisOptions(optsMap));
                    } catch (JsonProcessingException e) {
                        throw new ConfigException("Invalid analysis options: " +
                                entry.getKey() + ":" + entry.getValue(), e);
                    }
                })
                .toList();
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
                joiner.add(keyValue.substring(0, i) + ": "
                        + keyValue.substring(i + 1));
            }
        }
        return joiner.toString();
    }

    /**
     * Writes a list of PlanConfigs to given file.
     */
    public static void writeConfigs(List<PlanConfig> planConfigs, File outputDir) {
        File outFile = new File(outputDir, PLAN_FILE);
        ObjectMapper mapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            logger.info("Writing analysis plan to {}", outFile.getAbsolutePath());
            mapper.writeValue(outFile, planConfigs);
        } catch (IOException e) {
            throw new ConfigException("Failed to write plan file to "
                    + outFile.getAbsolutePath(), e);
        }
    }
}
