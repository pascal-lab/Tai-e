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

    /**
     * Description of the analysis.
     *
     * This information is only an explanation of the analysis,
     * and not used by Tai-e.
     */
    @JsonProperty
    private String description;

    /**
     * Fully-qualified name of the analysis class.
     *
     * Here we use String (class name) instead of the Class itself
     * to represent the analysis for fast startup speed. Our configuration
     * system will load all analyses in the configuration file at each start.
     * If we use Class for this field, then it needs to load all
     * analysis classes, including the ones that may not be used in this run,
     * which cost more time than merely loading class names.
     */
    @JsonProperty
    private String analysisClass;

    /**
     * Unique identifier of the analysis.
     *
     * Tai-e relies on analysis id to identify each analysis, so the id of
     * each analysis must be unique. If an id is assigned to multiple analyses,
     * the configuration system will throw {@link ConfigException}.
     */
    @JsonProperty
    private String id;

    /**
     * Require items of the analysis.
     *
     * Each require item contains two part:
     * 1. analysis id (say A), whose result is required by this analysis.
     * 2. require conditions, which are relevant to the options of this analysis.
     * If the conditions are given, then this analysis requires A
     * only when all conditions are satisfied.
     *
     * We support simple compositions of conditions, and we give some examples
     * to illustrate require items.
     * requires: [A1,A2] # requires analyses A1 and A2
     * requires: [A(x=y)] # requires A when value of option x is y
     * requires: [A(x=y&a=b)] # requires A when value of option x is y
     *   and value of option a is b
     * requires: [A(x=a|b|c)] # requires A when value of option x is
     *  a, b, or c.
     */
    @JsonProperty
    private List<String> requires = emptyList();

    /**
     * Options for the analysis.
     */
    @JsonProperty
    private Map<String, Object> options = newHybridMap();

    /**
     * Used by deserialization from configuration file.
     */
    public AnalysisConfig() {
    }

    /**
     * Constructing an AnalysisConfig by merely specifying id and options.
     * This convenient method eases the creation of AnalysisConfig in testing.
     * The given options should be an array of key-value pairs, e.g.,
     * [k1, v1, k2, v2, ...].
     */
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

    /**
     * Note that this API only returns unprocessed raw require information.
     * To obtain the real required analyses, you should call
     * {@link ConfigManager#getRequiredConfigs}.
     *
     * @return require information of this analysis given in configuration files.
     */
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
    public static List<AnalysisConfig> readConfigs(File file) {
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
