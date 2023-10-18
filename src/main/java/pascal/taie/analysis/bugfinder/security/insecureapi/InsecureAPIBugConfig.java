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

package pascal.taie.analysis.bugfinder.security.insecureapi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.analysis.bugfinder.BugType;
import pascal.taie.analysis.bugfinder.Severity;
import pascal.taie.analysis.bugfinder.security.SecurityBugType;
import pascal.taie.config.ConfigException;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Set;

class InsecureAPIBugConfig {

    private final Set<InsecureAPIBugInfo> bugSet;

    private InsecureAPIBugConfig(Set<InsecureAPIBugInfo> bugSet) {
        this.bugSet = bugSet;
    }

    public static InsecureAPIBugConfig readConfig(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        if(files == null) {
            throw new ConfigException("Failed to open insecure API analysis config directory " + directory);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(InsecureAPIBugConfig.class, new InsecureAPIBugConfig.Deserializer());
        mapper.registerModule(module);
        Set<InsecureAPIBugInfo> APIBugSet = Sets.newSet();

        for(File file : files) {
            try {
                InsecureAPIBugConfig config = mapper.readValue(file, InsecureAPIBugConfig.class);
                APIBugSet.addAll(config.getBugSet());
            } catch (IOException e) {
                throw new ConfigException("Failed to read insecure API analysis config file " + file, e);
            }
        }

        return new InsecureAPIBugConfig(APIBugSet);
    }

    Set<InsecureAPIBugInfo> getBugSet() { return bugSet; }

    private static class Deserializer extends JsonDeserializer<InsecureAPIBugConfig> {
        @Override
        public InsecureAPIBugConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            if(node instanceof ArrayNode arrayNode) {
                Set<InsecureAPIBugInfo> bugSet = Sets.newSet(arrayNode.size());
                for(JsonNode elem : arrayNode) {
                    bugSet.add(deserializeAPIBugInfo(elem));
                }
                return new InsecureAPIBugConfig(bugSet);
            } else {
                return new InsecureAPIBugConfig(Set.of());
            }

        }

        private InsecureAPIBugInfo deserializeAPIBugInfo(JsonNode node) {
            BugType bugType = new SecurityBugType(node.get("bugType").asText());
            Severity severity = Severity.valueOf(node.get("severity").asText());
            String description = node.get("description").asText();
            Set<InsecureAPI> apiSet = deserializeInsecureAPISet(node);
            return new InsecureAPIBugInfo(bugType, severity, description, apiSet);
        }

        private Set<InsecureAPI> deserializeInsecureAPISet(JsonNode node) {
            JsonNode apiList = node.get("methodList");
            if(apiList instanceof ArrayNode arrayNode) {
                Set<InsecureAPI> apiSet = Sets.newSet(arrayNode.size());
                for(JsonNode elem : arrayNode) {
                    String reference = elem.get("reference").asText();
                    String paramRegex = elem.get("parameter") != null ?
                            elem.get("parameter").asText() : null;
                    apiSet.add(new InsecureAPI(reference, paramRegex));
                }
                return apiSet;
            } else {
                return Set.of();
            }
        }
    }
}
