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
     * The empty AnalysisOptions.
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
     * to this AnalysisOptions. Only the given AnalysisOptions contain
     * value for the key that already exists in this AnalysisOptions,
     * then the old value can be overwritten, otherwise, exception
     * will be thrown.
     *
     * @throws IllegalArgumentException if exists a key of given AnalysisOptions
     *                                  not in this AnalysisOptions.
     */
    void update(AnalysisOptions options) {
        for (String key : options.options.keySet()) {
            if (!this.options.containsKey(key)) {
                throw new IllegalArgumentException("Illegal key of option '"
                        + key + ":" + options.options.get(key) + "'"
                        + ", you should specify a key that exists in the configuration");
            }
        }
        this.options.putAll(options.options);
    }

    /**
     * @return {@code true} if this AnalysisOptions contains value
     * for given option key.
     */
    public boolean has(String key) {
        return options.containsKey(key);
    }

    /**
     * @return value for given option key.
     * @throws ConfigException if this AnalysisOptions do not contain the key.
     */
    public Object get(String key) {
        if (!has(key)) {
            throw new ConfigException("Cannot find option '" + key + "'," +
                    " please check your configuration and option key");
        }
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
