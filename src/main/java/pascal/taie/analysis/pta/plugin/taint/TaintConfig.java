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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.ConfigException;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Configuration for taint analysis.
 */
class TaintConfig {

    private static final Logger logger = LogManager.getLogger(TaintConfig.class);

    /**
     * Set of sources.
     */
    private final Set<Source> sources;

    /**
     * Set of sinks.
     */
    private final Set<Sink> sinks;

    /**
     * Set of taint transfers;
     */
    private final Set<TaintTransfer> transfers;

    private TaintConfig(Set<Source> sources, Set<Sink> sinks,
                        Set<TaintTransfer> transfers) {
        this.sources = sources;
        this.sinks = sinks;
        this.transfers = transfers;
    }

    /**
     * Reads a taint analysis configuration from file
     *
     * @param path        the path to the config file
     * @param hierarchy   the class hierarchy
     * @param typeManager the type manager
     * @return the TaintConfig object
     * @throws ConfigException if failed to load the config file
     */
    static TaintConfig readConfig(
            String path, ClassHierarchy hierarchy, TypeManager typeManager) {
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TaintConfig.class,
                new Deserializer(hierarchy, typeManager));
        mapper.registerModule(module);
        try {
            return mapper.readValue(file, TaintConfig.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read taint analysis config file " + file, e);
        }
    }

    /**
     * @return sources in the configuration.
     */
    Set<Source> getSources() {
        return sources;
    }

    /**
     * @return sinks in the configuration.
     */
    Set<Sink> getSinks() {
        return sinks;
    }

    /**
     * @return taint transfers in the configuration.
     */
    Set<TaintTransfer> getTransfers() {
        return transfers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TaintConfig:");
        if (!sources.isEmpty()) {
            sb.append("\nsources:\n");
            sources.forEach(source ->
                    sb.append("  ").append(source).append("\n"));
        }
        if (!sinks.isEmpty()) {
            sb.append("\nsinks:\n");
            sinks.forEach(sink ->
                    sb.append("  ").append(sink).append("\n"));
        }
        if (!transfers.isEmpty()) {
            sb.append("\ntransfers:\n");
            transfers.forEach(transfer ->
                    sb.append("  ").append(transfer).append("\n"));
        }
        return sb.toString();
    }

    /**
     * Deserializer for {@link TaintConfig}.
     */
    private static class Deserializer extends JsonDeserializer<TaintConfig> {

        private final ClassHierarchy hierarchy;

        private final TypeManager typeManager;

        private Deserializer(ClassHierarchy hierarchy, TypeManager typeManager) {
            this.hierarchy = hierarchy;
            this.typeManager = typeManager;
        }

        @Override
        public TaintConfig deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            Set<Source> sources = deserializeSources(node.get("sources"));
            Set<Sink> sinks = deserializeSinks(node.get("sinks"));
            Set<TaintTransfer> transfers = deserializeTransfers(node.get("transfers"));
            return new TaintConfig(sources, sinks, transfers);
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link Source}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link Source}
         */
        private Set<Source> deserializeSources(JsonNode node) {
            if (node instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) node;
                Set<Source> sources = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        Type type = typeManager.getType(
                                elem.get("type").asText());
                        sources.add(new Source(method, type));
                    } else {
                        logger.warn("Cannot find source method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(sources);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link Sink}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link Sink}
         */
        private Set<Sink> deserializeSinks(JsonNode node) {
            if (node instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) node;
                Set<Sink> sinks = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int index = elem.get("index").asInt();
                        sinks.add(new Sink(method, index));
                    } else {
                        logger.warn("Cannot find sink method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(sinks);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link TaintTransfer}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link TaintTransfer}
         */
        private Set<TaintTransfer> deserializeTransfers(JsonNode node) {
            if (node instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) node;
                Set<TaintTransfer> transfers = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int from = TaintTransfer.toInt(elem.get("from").asText());
                        int to = TaintTransfer.toInt(elem.get("to").asText());
                        Type type = typeManager.getType(
                                elem.get("type").asText());
                        transfers.add(new TaintTransfer(method, from, to, type));
                    } else {
                        logger.warn("Cannot find taint-transfer method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(transfers);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }
    }
}
