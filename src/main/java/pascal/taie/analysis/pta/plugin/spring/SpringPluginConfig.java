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

package pascal.taie.analysis.pta.plugin.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum SpringPluginConfig {

    INSTANCE;

    private static final String CONFIG_FILE = "spring-plugin-config.yaml";

    // ----------------------------------- Dependency Injection -----------------------------------

    private final List<DiXmlTag> diXmlTags;

    private final List<String> diClassAnnos;

    private final List<String> diFactoryMethodAnnos;

    private final List<String> diClassCtorAnnos;

    private final List<String> injectedFieldAnnos;

    private final List<String> injectedMethodAnnos;

    private final List<String> qualifierAnnos;

    // --------------------------------- Web Endpoint Configuration ---------------------------------

    private final List<String> endpointClassAnnos;

    private final List<String> endpointMetadataClassAnnos;

    private final List<String> endpointMetadataMethodAnnos;

    private final List<String> endpointParameterAnnos;

    SpringPluginConfig() {
        diXmlTags = new ArrayList<>();
        diClassAnnos = new ArrayList<>();
        diFactoryMethodAnnos = new ArrayList<>();
        diClassCtorAnnos = new ArrayList<>();
        injectedFieldAnnos = new ArrayList<>();
        qualifierAnnos = new ArrayList<>();
        injectedMethodAnnos = new ArrayList<>();
        endpointClassAnnos = new ArrayList<>();
        endpointMetadataClassAnnos = new ArrayList<>();
        endpointMetadataMethodAnnos = new ArrayList<>();
        endpointParameterAnnos = new ArrayList<>();
        loadConfig();
    }

    private void loadConfig() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            InputStream is = SpringPluginConfig.class.getClassLoader()
                    .getResourceAsStream(CONFIG_FILE);
            if (is == null) {
                throw new RuntimeException("Configuration file '"
                        + CONFIG_FILE + "' not found on classpath");
            }
            JsonNode rootNode = mapper.readTree(is);

            JsonNode diArray = rootNode.get("dependency-injection");
            for (JsonNode item : diArray) {
                if (item.has("di-xml-tags")) {
                    JsonNode xmlTagArray = item.get("di-xml-tags");
                    for (JsonNode tagNode : xmlTagArray) {
                        String tagName = tagNode.get("tag-name").asText();
                        String classAttr = tagNode.get("class-attribute").asText();
                        String idAttr = tagNode.get("id-attribute").asText();
                        diXmlTags.add(new DiXmlTag(tagName, classAttr, idAttr));
                    }
                }
                if (item.has("di-class-annotations")) {
                    JsonNode classAnnoArray = item.get("di-class-annotations");
                    for (JsonNode annoNode : classAnnoArray) {
                        diClassAnnos.add(annoNode.asText());
                    }
                }
                if (item.has("di-factory-method-annotations")) {
                    JsonNode factoryMethodAnnoArray = item.get("di-factory-method-annotations");
                    for (JsonNode annoNode : factoryMethodAnnoArray) {
                        diFactoryMethodAnnos.add(annoNode.asText());
                    }
                }
                if (item.has("di-class-constructor-annotations")) {
                    JsonNode ctorAnnoArray = item.get("di-class-constructor-annotations");
                    for (JsonNode annoNode : ctorAnnoArray) {
                        diClassCtorAnnos.add(annoNode.asText());
                    }
                }
                if (item.has("injected-field-annotations")) {
                    JsonNode injectedFieldAnnoArray = item.get("injected-field-annotations");
                    for (JsonNode injectedFieldAnno : injectedFieldAnnoArray) {
                        injectedFieldAnnos.add(injectedFieldAnno.asText());
                    }
                }
                if (item.has("injected-method-annotations")) {
                    JsonNode injectedMethodAnnoArray = item.get("injected-method-annotations");
                    for (JsonNode injectedMethodAnno : injectedMethodAnnoArray) {
                        injectedMethodAnnos.add(injectedMethodAnno.asText());
                    }
                }
                if (item.has("qualifier-annotations")) {
                    JsonNode qualifierAnnoArray = item.get("qualifier-annotations");
                    for (JsonNode qualifierAnno : qualifierAnnoArray) {
                        qualifierAnnos.add(qualifierAnno.asText());
                    }
                }
            }

            JsonNode wecArray = rootNode.get("web-endpoint-configuration");
            for (JsonNode item : wecArray) {
                if (item.has("endpoint-class-annotations")) {
                    JsonNode classAnnoArray = item.get("endpoint-class-annotations");
                    for (JsonNode annoNode : classAnnoArray) {
                        endpointClassAnnos.add(annoNode.asText());
                    }
                }
                if (item.has("endpoint-metadata-class-annotations")) {
                    JsonNode metadataClassAnnoArray = item.get("endpoint-metadata-class-annotations");
                    for (JsonNode annoNode : metadataClassAnnoArray) {
                        endpointMetadataClassAnnos.add(annoNode.asText());
                    }
                }
                if (item.has("endpoint-metadata-method-annotations")) {
                    JsonNode metadataMethodAnnoArray = item.get("endpoint-metadata-method-annotations");
                    for (JsonNode annoNode : metadataMethodAnnoArray) {
                        endpointMetadataMethodAnnos.add(annoNode.asText());
                    }
                }
                if (item.has("endpoint-parameter-annotations")) {
                    JsonNode parameterAnnoArray = item.get("endpoint-parameter-annotations");
                    for (JsonNode annoNode : parameterAnnoArray) {
                        endpointParameterAnnos.add(annoNode.asText());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse configuration file", e);
        }
    }

    public List<DiXmlTag> getDiXmlTags() {
        return Collections.unmodifiableList(diXmlTags);
    }

    public List<String> getDiClassAnnos() {
        return Collections.unmodifiableList(diClassAnnos);
    }

    public List<String> getDiFactoryMethodAnnos() {
        return Collections.unmodifiableList(diFactoryMethodAnnos);
    }

    public List<String> getDiClassCtorAnnos() {
        return Collections.unmodifiableList(diClassCtorAnnos);
    }

    public List<String> getInjectedFieldAnnos() {
        return Collections.unmodifiableList(injectedFieldAnnos);
    }

    public List<String> getInjectedMethodAnnos() {
        return Collections.unmodifiableList(injectedMethodAnnos);
    }

    public List<String> getQualifierAnnos() {
        return Collections.unmodifiableList(qualifierAnnos);
    }

    public List<String> getEndpointClassAnnos() {
        return Collections.unmodifiableList(endpointClassAnnos);
    }

    public List<String> getEndpointMetadataClassAnnos() {
        return Collections.unmodifiableList(endpointMetadataClassAnnos);
    }

    public List<String> getEndpointMetadataMethodAnnos() {
        return Collections.unmodifiableList(endpointMetadataMethodAnnos);
    }

    public List<String> getEndpointParameterAnnos() {
        return Collections.unmodifiableList(endpointParameterAnnos);
    }

    public static record DiXmlTag(String tagName, String classAttr, String idAttr) {
    }

}
