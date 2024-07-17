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

package pascal.taie.analysis.pta.plugin.taint;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.config.ConfigException;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.SignatureMatcher;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static pascal.taie.analysis.pta.plugin.taint.IndexRef.ARRAY_SUFFIX;

/**
 * Configuration for taint analysis.
 */
public record TaintConfig(List<Source> sources,
                          List<Sink> sinks,
                          List<TaintTransfer> transfers,
                          List<ParamSanitizer> paramSanitizers,
                          boolean callSiteMode) {

    /**
     * An empty taint config.
     */
    public static final TaintConfig EMPTY = new TaintConfig(
            List.of(), List.of(), List.of(), List.of(), false);

    /**
     * Merges this taint config with other taint config.
     * @return a new merged taint config.
     */
    public TaintConfig mergeWith(TaintConfig other) {
        return new TaintConfig(
                Lists.concatDistinct(sources, other.sources),
                Lists.concatDistinct(sinks, other.sinks),
                Lists.concatDistinct(transfers, other.transfers),
                Lists.concatDistinct(paramSanitizers, other.paramSanitizers),
                callSiteMode || other.callSiteMode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TaintConfig:");
        if (!sources.isEmpty()) {
            sb.append("\nsources:\n");
            sources.forEach(source ->
                    sb.append("  - ").append(source).append("\n"));
        }
        if (!sinks.isEmpty()) {
            sb.append("\nsinks:\n");
            sinks.forEach(sink ->
                    sb.append("  - ").append(sink).append("\n"));
        }
        if (!transfers.isEmpty()) {
            sb.append("\ntransfers:\n");
            transfers.forEach(transfer ->
                    sb.append("  - ").append(transfer).append("\n"));
        }
        if (!paramSanitizers.isEmpty()) {
            sb.append("\nsanitizers:\n");
            paramSanitizers.forEach(sanitizer ->
                    sb.append("  - ").append(sanitizer).append("\n"));
        }
        if (callSiteMode) {
            sb.append("\ncallSiteMode: true\n");
        }
        return sb.toString();
    }

}
