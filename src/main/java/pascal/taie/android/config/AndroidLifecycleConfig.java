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

package pascal.taie.android.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.config.ConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Configuration for an AndroidLifecycle.
 */
public record AndroidLifecycleConfig(@JsonProperty String className,
                                     @JsonProperty List<String> callbackMethodSubSigs) {

    private static final String CONFIG = "android/android-lifecycle.yml";

    /**
     * Used by deserialization from configuration file.
     */
    @JsonCreator
    public AndroidLifecycleConfig(
            @JsonProperty("className") String className,
            @JsonProperty("callbackMethodSubSigs") List<String> callbackMethodSubSigs) {
        this.className = className;
        this.callbackMethodSubSigs = callbackMethodSubSigs;
    }

    @Override
    public String toString() {
        return "AndroidLifecycleConfig{" +
                "className='" + className + '\'' +
                ", callbackMethodSubSigs='" + callbackMethodSubSigs + '\'' +
                '}';
    }

    /**
     * Parses a list of AndroidLifecycleConfig from given input stream.
     */
    public static List<AndroidLifecycleConfig> loadAndroidLifecycleConfig() {
        InputStream content = AndroidLifecycleConfig.class
                .getClassLoader()
                .getResourceAsStream(CONFIG);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, AndroidLifecycleConfig.class);
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new ConfigException("Failed to read AndroidLifecycle config file", e);
        }
    }

}
