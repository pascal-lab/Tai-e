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

package pascal.taie.android.info;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts manifest {@code <intent-filter>} nodes to {@link IntentFilterAttribute}s.
 */
final class ManifestIntentFilterResolver {

    private static final String TAG_ACTION = "action";

    private static final String TAG_CATEGORY = "category";

    private static final String TAG_DATA = "data";

    private static final String TAG_INTENT_FILTER = "intent-filter";

    private static final String ATTR_ENABLED = "enabled";

    private static final String ATTR_NAME = "name";

    private static final String DATA_SCHEME = "scheme";

    private static final String DATA_HOST = "host";

    private static final String DATA_PORT = "port";

    private static final String DATA_PATH = "path";

    private static final String DATA_PATH_PREFIX = "pathPrefix";

    private static final String DATA_PATH_SUFFIX = "pathSuffix";

    private static final String DATA_PATH_PATTERN = "pathPattern";

    private static final String DATA_PATH_ADVANCED_PATTERN = "pathAdvancedPattern";

    private static final String DATA_MIME_TYPE = "mimeType";

    private final RawApkInfo rawApkInfo;

    private final ProcessManifest manifest;

    private final ClassHierarchy hierarchy;

    ManifestIntentFilterResolver(RawApkInfo rawApkInfo,
                                 ProcessManifest manifest,
                                 ClassHierarchy hierarchy) {
        this.rawApkInfo = rawApkInfo;
        this.manifest = manifest;
        this.hierarchy = hierarchy;
    }

    MultiMap<JClass, IntentFilterAttribute> convert() {
        return Maps.newMultiMap(manifest
                .getAXml()
                .getNodesWithTag(TAG_INTENT_FILTER)
                .stream()
                .filter(this::isEnabled)
                .filter(intentFilter -> {
                    String componentName = getParentName(intentFilter);
                    return componentName != null && hierarchy.getClass(componentName) != null;
                })
                .collect(Collectors.groupingBy(
                        intentFilter -> Objects.requireNonNull(
                                hierarchy.getClass(getParentName(intentFilter))),
                        Collectors.mapping(
                                intentFilter -> new IntentFilterAttribute(
                                        Set.of(Objects.requireNonNull(getParentName(intentFilter))),
                                        extractActions(intentFilter),
                                        extractCategories(intentFilter),
                                        createIntentData(extractDataAttributes(intentFilter))),
                                Collectors.toSet()
                        )
                )));
    }

    /**
     * Intent-filters of disabled components should not participate in
     * component matching.
     */
    private boolean isEnabled(AXmlNode intentFilter) {
        AXmlNode component = intentFilter.getParent();
        if (component == null) {
            return false;
        }
        AXmlAttribute<?> enabled = component.getAttribute(ATTR_ENABLED);
        return enabled == null || enabled.asBoolean(rawApkInfo.resources());
    }

    private Set<String> extractActions(AXmlNode intentFilter) {
        return extractChildNames(intentFilter, TAG_ACTION);
    }

    private Set<String> extractCategories(AXmlNode intentFilter) {
        return extractChildNames(intentFilter, TAG_CATEGORY);
    }

    /**
     * Extracts the values of {@code android:name} from children with the
     * given tag, e.g., {@code <action>} and {@code <category>}.
     */
    private Set<String> extractChildNames(AXmlNode parent, String tag) {
        return parent.getChildren()
                .stream()
                .filter(node -> tag.equals(node.getTag()))
                .map(ManifestIntentFilterResolver::getAttributeValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Groups all attributes of {@code <data>} nodes by attribute name, so
     * they can be converted to {@link IntentDataInfo}.
     */
    private MultiMap<String, String> extractDataAttributes(AXmlNode intentFilter) {
        return Maps.newMultiMap(intentFilter.getChildren()
                .stream()
                .filter(node -> TAG_DATA.equals(node.getTag()))
                .flatMap(node -> node.getAttributes().entrySet().stream())
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(
                                entry -> entry.getValue().getValue().toString(),
                                Collectors.toSet())
                )));
    }

    /**
     * Converts flattened data attributes in manifest to structured URI data
     * used by intent matching.
     */
    private Set<UriData> createIntentData(MultiMap<String, String> dataAttributes) {
        return new IntentDataInfo(
                dataAttributes.get(DATA_SCHEME),
                dataAttributes.get(DATA_HOST),
                dataAttributes.get(DATA_PORT),
                dataAttributes.get(DATA_PATH),
                dataAttributes.get(DATA_PATH_PREFIX),
                dataAttributes.get(DATA_PATH_SUFFIX),
                dataAttributes.get(DATA_PATH_PATTERN),
                dataAttributes.get(DATA_PATH_ADVANCED_PATTERN),
                dataAttributes.get(DATA_MIME_TYPE)).convertToDataSet();
    }

    @Nullable
    private String getParentName(AXmlNode intentFilter) {
        AXmlNode parent = intentFilter.getParent();
        String name = parent == null ? null : getAttributeValue(parent);
        return name == null ? null : manifest.expandClassName(name);
    }

    @Nullable
    private static String getAttributeValue(AXmlNode node) {
        return getAttributeValue(node.getAttribute(ATTR_NAME));
    }

    @Nullable
    private static String getAttributeValue(@Nullable AXmlAttribute<?> attribute) {
        if (attribute == null || attribute.getValue() == null) {
            return null;
        }
        return attribute.getValue().toString();
    }
}
