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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static pascal.taie.util.collection.CollectionUtils.newHybridMap;

/**
 * Configuration for an analysis.
 */
public class AnalysisConfig {

    @JsonProperty
    private String description;

    @JsonProperty
    private String analysisClass;

    @JsonProperty
    private String id;

    @JsonProperty
    private List<String> requires = emptyList();

    @JsonProperty
    private Map<String, Object> options = newHybridMap();

    /**
     * Used by deserialization from configuration file.
     */
    public AnalysisConfig() {
    }

    public AnalysisConfig(String id, Object... options) {
        this.id = id;
        for (int i = 0; i < options.length; i += 2) {
            this.options.put((String) options[i], options[i + 1]);
        }
    }

    public String getDescription() {
        return description;
    }

    public String getAnalysisClass() {
        return analysisClass;
    }

    public String getId() {
        return id;
    }

    List<String> getRequires() {
        return requires;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public String toDetailedString() {
        return "AnalysisConfig{" +
                "description='" + description + '\'' +
                ", analysisClass='" + analysisClass + '\'' +
                ", id='" + id + '\'' +
                ", requires=" + requires +
                ", options=" + options +
                '}';
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Read a list of AnalysisConfig from given file.
     */
    public static List<AnalysisConfig> readFromFile(File file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, AnalysisConfig.class);
        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            throw new ConfigException("Failed to read analysis config file " + file, e);
        }
    }
}
