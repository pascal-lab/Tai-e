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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.Options2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.CollectionUtils.newHybridMap;

public class PlanConfig {

    @JsonProperty
    private String id;

    @JsonProperty
    private Map<String, Object> options = newHybridMap();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
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
    public static List<PlanConfig> readFromFile(File file) {
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
     * Read a list of PlanConfig from command-line options.
     */
    public static List<PlanConfig> readFromOptions(Options2 options) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType mapType = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        return options.getAnalyses().entrySet()
                .stream()
                .map(entry -> {
                    String id = entry.getKey();
                    // Convert option string to a valid YAML string
                    String opts = entry.getValue()
                            .replace(',', '\n')
                            .replace(":", ": ");
                    PlanConfig config = new PlanConfig();
                    config.setId(id);
                    try {
                        if (!opts.isEmpty()) {
                            // Leverage Jackson to parse YAML string to Map
                            config.setOptions(mapper.readValue(opts, mapType));
                        }
                        return config;
                    } catch (JsonProcessingException e) {
                        throw new ConfigException("Invalid analysis options: " +
                                entry.getKey() + ":" + entry.getValue(), e);
                    }
                })
                .collect(Collectors.toUnmodifiableList());
    }
}
