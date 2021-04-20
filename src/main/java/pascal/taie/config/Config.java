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

import java.util.List;
import java.util.Map;

public class Config {

    private String analysisClass;

    private String resultClass;

    private String shortName;

    private List<String> requires;

    @JsonProperty
    private Map<String, Object> options;

    public String getAnalysisClass() {
        return analysisClass;
    }

    public String getResultClass() {
        return resultClass;
    }

    public String getShortName() {
        return shortName;
    }

    public List<String> getRequires() {
        return requires;
    }

    public Object get(String key) {
        return options.get(key);
    }

    public Object get(String key, Object defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    public String getString(String key) {
        return (String) options.get(key);
    }

    public String getString(String key, String defaultValue) {
        return (String) options.getOrDefault(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return (Boolean) options.get(key);
    }

    public boolean getBoolean(String key, int defaultValue) {
        return (Boolean) options.getOrDefault(key, defaultValue);
    }

    public int getInt(String key) {
        return (Integer) options.get(key);
    }

    public int getInt(String key, int defaultValue) {
        return (Integer) options.getOrDefault(key, defaultValue);
    }

    @Override
    public String toString() {
        return "Config{" +
                "analysisClass='" + analysisClass + '\'' +
                ", resultClass='" + resultClass + '\'' +
                ", shortName='" + shortName + '\'' +
                ", requires=" + requires +
                ", options=" + options +
                '}';
    }
}
