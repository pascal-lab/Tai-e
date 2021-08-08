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
import pascal.taie.config.ConfigException;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.SetUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Configuration for taint analysis.
 */
class TaintConfig {

    /**
     * Set of source methods.
     */
    private final Set<JMethod> sources;

    /**
     * Set of sink methods.
     */
    private final Set<MethodParam> sinks;

    /**
     * Set of methods that could transfer taint from base object to return value.
     */
    private final Set<JMethod> baseToRet;

    /**
     * Set of methods that could transfer taint from parameter to return value.
     */
    private final Set<MethodParam> paramToRet;

    /**
     * Set of methods that could transfer taint from parameter to base object.
     */
    private final Set<MethodParam> paramToBase;

    private TaintConfig(Set<JMethod> sources, Set<MethodParam> sinks,
                       Set<JMethod> baseToRet, Set<MethodParam> paramToRet,
                       Set<MethodParam> paramToBase) {
        this.sources = sources;
        this.sinks = sinks;
        this.baseToRet = baseToRet;
        this.paramToRet = paramToRet;
        this.paramToBase = paramToBase;
    }

    Set<JMethod> getSources() {
        return sources;
    }

    Set<MethodParam> getSinks() {
        return sinks;
    }

    Set<JMethod> getBaseToRet() {
        return baseToRet;
    }

    Set<MethodParam> getParamToRet() {
        return paramToRet;
    }

    Set<MethodParam> getParamToBase() {
        return paramToBase;
    }

    @Override
    public String toString() {
        return "TaintConfig{" +
                "sources=" + sources +
                ", sinks=" + sinks +
                ", baseToRet=" + baseToRet +
                ", paramToRet=" + paramToRet +
                ", paramToBase=" + paramToBase +
                '}';
    }

    /**
     * Reads a taint analysis configuration from file
     * @param path the path to the config file
     * @param hierarchy the class hierarchy
     * @return the TaintConfig object
     * @throws ConfigException if failed to load the config file
     */
    static TaintConfig readConfig(String path, ClassHierarchy hierarchy) {
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TaintConfig.class, new Deserializer(hierarchy));
        mapper.registerModule(module);
        try {
            return mapper.readValue(file, TaintConfig.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read taint analysis config file " + file, e);
        }
    }

    /**
     * Deserializer for {@link TaintConfig}.
     */
    private static class Deserializer extends JsonDeserializer<TaintConfig> {

        private final ClassHierarchy hierarchy;

        private Deserializer(ClassHierarchy hierarchy) {
            this.hierarchy = hierarchy;
        }

        @Override
        public TaintConfig deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            Set<JMethod> sources = deserializeMethods(node.get("sources"));
            Set<MethodParam> sinks = deserializeMethodParams(node.get("sinks"));
            Set<JMethod> baseToRet = deserializeMethods(node.get("baseToRet"));
            Set<MethodParam> paramToRet = deserializeMethodParams(node.get("paramToRet"));
            Set<MethodParam> paramToBase = deserializeMethodParams(node.get("paramToBase"));
            return new TaintConfig(sources, sinks, baseToRet, paramToRet, paramToBase);
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link JMethod}.
         * @param node the node to be deserialized
         * @return set of deserialized {@link JMethod}
         */
        private Set<JMethod> deserializeMethods(JsonNode node) {
            if (node instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) node;
                Set<JMethod> methods = SetUtils.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        methods.add(method);
                    }
                }
                return Collections.unmodifiableSet(methods);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link MethodParam}.
         * @param node the node to be deserialized
         * @return set of deserialized {@link MethodParam}
         */
        private Set<MethodParam> deserializeMethodParams(JsonNode node) {
            if (node instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) node;
                Set<MethodParam> methodParams = SetUtils.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int index = elem.get("index").asInt();
                        methodParams.add(new MethodParam(method, index));
                    }
                }
                return Collections.unmodifiableSet(methodParams);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }
    }
}
