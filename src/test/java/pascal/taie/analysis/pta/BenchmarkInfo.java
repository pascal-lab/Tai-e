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

package pascal.taie.analysis.pta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.util.collection.Maps;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record BenchmarkInfo(String id, int jdk,
                     String main, List<String> apps, List<String> libs,
                     String reflectionLog, boolean allowPhantom) {
    @JsonCreator
    public BenchmarkInfo(
            @JsonProperty("id") String id,
            @JsonProperty("jdk") int jdk,
            @JsonProperty("main") String main,
            @JsonProperty("apps") List<String> apps,
            @JsonProperty("libs") List<String> libs,
            @JsonProperty("refl-log") String reflectionLog,
            @JsonProperty("phantom") boolean allowPhantom) {
        this.id = id;
        this.jdk = jdk;
        this.main = main;
        this.apps = Objects.requireNonNullElse(apps, List.of());
        this.libs = Objects.requireNonNullElse(libs, List.of());
        this.reflectionLog = reflectionLog;
        this.allowPhantom = allowPhantom;
    }

    static Map<String, BenchmarkInfo> load(String path) {
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TypeReference<List<BenchmarkInfo>> typeRef = new TypeReference<>() {};
        try {
            Map<String, BenchmarkInfo> benchmarkInfos = Maps.newLinkedHashMap();
            mapper.readValue(file, typeRef).forEach(
                    bmInfo -> benchmarkInfos.put(bmInfo.id(), bmInfo));
            return benchmarkInfos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
