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
     * Copies all the options from the specified AnalysisOptions
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

    public boolean getBooleanOrDefault(String key, boolean defaultValue) {
        return (Boolean) options.getOrDefault(key, defaultValue);
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
