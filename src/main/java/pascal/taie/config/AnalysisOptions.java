/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper class for analysis options.
 * Each instance wraps the options (represented by a Map) for an analysis,
 * and provides convenient APIs to access various types of option values.
 */
@JsonSerialize(using = AnalysisOptions.Serializer.class)
public class AnalysisOptions {

    /**
     * Uses Collections.emptyMap() instead of Map.of() to avoid
     * UnsupportedOperationException: {@link ConfigManager#overwriteOptions}
     * unconditionally updates this map, thus we uses emptyMap() here
     * for empty options to avoid UOE.
     */
    private static final AnalysisOptions EMPTY_OPTIONS =
            new AnalysisOptions(Collections.emptyMap());

    private final Map<String, Object> options;

    /**
     * @return an unmodifiable empty AnalysisOptions containing no options.
     */
    static AnalysisOptions emptyOptions() {
        return EMPTY_OPTIONS;
    }

    @JsonCreator
    public AnalysisOptions(Map<String, Object> options) {
        this.options = Objects.requireNonNull(options);
    }

    /**
     * Copies all of the options from the specified AnalysisOptions
     * to this AnalysisOptions.
     */
    void update(AnalysisOptions options) {
        this.options.putAll(options.options);
    }

    public Object get(String key) {
        return options.get(key);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public boolean getBoolean(String key) {
        return (Boolean) get(key);
    }

    public int getInt(String key) {
        return (Integer) get(key);
    }

    public float getFloat(String key) {
        return (Float) get(key);
    }

    @Override
    public String toString() {
        return "AnalysisOptions" + options;
    }

    /**
     * Serializer for AnalysisOptions, which serializes each AnalysisOptions
     * object as a map.
     */
    static class Serializer extends JsonSerializer<AnalysisOptions> {

        @Override
        public void serialize(
                AnalysisOptions value, JsonGenerator gen,
                SerializerProvider serializers) throws IOException {
            gen.writeObject(value.options);
        }
    }
}
