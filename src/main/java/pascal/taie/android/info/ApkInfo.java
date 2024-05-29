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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.AbstractWorldBuilder;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.IAndroidApplication;
import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.android.AndroidClassNames.ANDROIDX_FRAGMENT;
import static pascal.taie.android.AndroidClassNames.FRAGMENT;
import static pascal.taie.android.AndroidClassNames.VIEW;

public record ApkInfo(JClass application,
                      Set<JClass> exportedActivities,
                      Set<JClass> exportedServices,
                      Set<JClass> exportedBroadcastReceivers,
                      Set<JClass> exportedContentProviders,
                      Set<JClass> enabledActivities,
                      Set<JClass> enabledServices,
                      Set<JClass> enabledBroadcastReceivers,
                      Set<JClass> enabledContentProviders,
                      MultiMap<String, Subsignature> layoutCallbacks,
                      MultiMap<String, JClass> layoutFragments,
                      MultiMap<String, JClass> layoutViews,
                      MultiMap<Type, JMethod> androidCallbacks,
                      MultiMap<JClass, TransferFilterInfo> componentFilterInfo,
                      RawApkInfo rawApkInfo) {

    public Set<JClass> getEnabledComponents() {
        if (!enabledApplication()) {
            return Sets.newSet();
        }

        Set<JClass> components = Stream.of(enabledActivities,
                        enabledServices,
                        enabledBroadcastReceivers,
                        enabledContentProviders)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        if (application != null) {
            components.add(application);
        }
        return components;
    }

    public Set<JClass> getEntrypointClasses() {
        return getEnabledComponents();
    }

    public ARSCFileParser getResources() {
        return rawApkInfo.resources();
    }

    public String getPackageName() {
        return rawApkInfo.manifest().getPackageName();
    }

    public int getTargetSdkVersion() {
        return rawApkInfo.manifest().getTargetSdkVersion();
    }

    public Set<String> getPermissions() {
        return rawApkInfo.manifest().getPermissions();
    }

    public List<AXmlNode> getManifestAXmlNodesWithTag(String tag) {
        return rawApkInfo.manifest().getAXml().getNodesWithTag(tag);
    }

    public ARSCFileParser.AbstractResource findResource(int resourceId) {
        return rawApkInfo.resources().findResource(resourceId);
    }

    public boolean enabledApplication() {
        IAndroidApplication app = rawApkInfo.manifest().getApplication();
        return app == null || app.isEnabled();
    }

    public static class ApkInfoConverter {

        private static final String ANDROID_CALLBACKS = "android-callbacks.yml";

        private final RawApkInfo rawApkInfo;

        private final ClassHierarchy hierarchy;

        public ApkInfoConverter(RawApkInfo rawApkInfo, ClassHierarchy hierarchy) {
            this.rawApkInfo = rawApkInfo;
            this.hierarchy = hierarchy;
        }

        public JClass convertApplication() {
            return Optional.ofNullable(rawApkInfo.manifest().getApplication())
                    .filter(IAndroidApplication::isEnabled)
                    .map(IAndroidApplication::getName)
                    .map(hierarchy::getClass)
                    .orElse(null);
        }

        public Set<JClass> convertExportedActivities() {
            return getComponentsByFilter(rawApkInfo.manifest().getActivities().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertExportedServices() {
            return getComponentsByFilter(rawApkInfo.manifest().getServices().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertExportedBroadcastReceivers() {
            return getComponentsByFilter(rawApkInfo.manifest().getBroadcastReceivers().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertExportedContentProviders() {
            return getComponentsByFilter(rawApkInfo.manifest().getContentProviders().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertEnabledActivities() {
            return getComponentsByFilter(rawApkInfo.manifest().getActivities().asList(), IAndroidComponent::isEnabled);
        }

        public Set<JClass> convertEnabledServices() {
            return getComponentsByFilter(rawApkInfo.manifest().getServices().asList(), IAndroidComponent::isEnabled);
        }

        public Set<JClass> convertEnabledBroadcastReceivers() {
            return getComponentsByFilter(rawApkInfo.manifest().getBroadcastReceivers().asList(), IAndroidComponent::isEnabled);
        }

        public Set<JClass> convertEnabledContentProviders() {
            return getComponentsByFilter(rawApkInfo.manifest().getContentProviders().asList(), IAndroidComponent::isEnabled);
        }

        private Set<JClass> getComponentsByFilter(List<? extends IAndroidComponent> components,
                                                 Predicate<IAndroidComponent> filterPredicate) {
            return components.stream()
                    .filter(filterPredicate)
                    .filter(c -> c.getNameString() != null && hierarchy.getClass(c.getNameString()) != null)
                    .map(c -> hierarchy.getClass(c.getNameString()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        public MultiMap<String, Subsignature> convertLayoutCallbacks() {
            MultiMap<String, Subsignature> layoutCallbacks = Maps.newMultiMap();
            rawApkInfo.layoutFile().getCallbackMethods().forEach(pair ->
                    layoutCallbacks.put(pair.getO1(), Subsignature.get("void " + pair.getO2() + "(android.view.View)"))
            );
            return layoutCallbacks;
        }

        public MultiMap<String, JClass> convertLayoutFragments() {
            return getLayoutComponents(
                    List.of(Objects.requireNonNull(hierarchy.getClass(FRAGMENT)),
                            Objects.requireNonNull(hierarchy.getClass(ANDROIDX_FRAGMENT))),
                    rawApkInfo.layoutFile().getFragments());
        }

        public MultiMap<String, JClass> convertLayoutViews() {
            return getLayoutComponents(
                    List.of(Objects.requireNonNull(hierarchy.getClass(VIEW))),
                    rawApkInfo.layoutFile().getViews());
        }

        public MultiMap<Type, JMethod> convertAndroidCallbacks() {
            return Maps.newMultiMap(loadAndroidCallbacks()
                    .stream()
                    .map(hierarchy::getClass)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            JClass::getType,
                            jClass -> jClass.getDeclaredMethods()
                                    .stream()
                                    .filter(m -> !m.isConstructor())
                                    .collect(Collectors.toSet())
                    )));
        }

        public MultiMap<JClass, TransferFilterInfo> convertComponentFilterInfo() {
            return Maps.newMultiMap(rawApkInfo.manifest()
                    .getAXml()
                    .getNodesWithTag("intent-filter")
                    .stream()
                    .filter(this::isEnabled)
                    .filter(intentFilter -> {
                        String componentName = getParentName(intentFilter);
                        return componentName != null && hierarchy.getClass(componentName) != null;
                    })
                    .collect(Collectors.groupingBy(
                    intentFilter -> Objects.requireNonNull(hierarchy.getClass(getParentName(intentFilter))),
                    Collectors.mapping(
                            intentFilter -> new TransferFilterInfo(
                                    Set.of(Objects.requireNonNull(getParentName(intentFilter))),
                                    extractActions(intentFilter),
                                    extractCategories(intentFilter),
                                    createTransferDataInfo(extractDataMap(intentFilter))),
                            Collectors.toSet()
                    )
            )));
        }

        private Set<String> extractActions(AXmlNode intentFilter) {
            return intentFilter.getChildren()
                    .stream()
                    .filter(node -> node.getTag().equals("action"))
                    .filter(node -> node.getAttribute("name") != null)
                    .map(node -> node.getAttribute("name").getValue().toString())
                    .collect(Collectors.toSet());
        }

        private Set<String> extractCategories(AXmlNode intentFilter) {
            return intentFilter.getChildren()
                    .stream()
                    .filter(node -> node.getTag().equals("category"))
                    .filter(node -> node.getAttribute("name") != null)
                    .map(categoryNode -> categoryNode.getAttribute("name").getValue().toString())
                    .collect(Collectors.toSet());
        }

        private MultiMap<String, String> extractDataMap(AXmlNode intentFilter) {
            return Maps.newMultiMap(intentFilter.getChildren()
                    .stream()
                    .filter(node -> node.getTag().equals("data"))
                    .flatMap(node -> node.getAttributes().entrySet().stream())
                    .filter(entry -> entry.getValue() != null)
                    .collect(
                            Collectors.groupingBy(
                                    Map.Entry::getKey,
                                    Collectors.mapping(entry -> entry.getValue().getValue().toString(), Collectors.toSet())
                            )
                    ));
        }

        private Set<UriData> createTransferDataInfo(MultiMap<String, String> dataMap) {
            return new TransferDataInfo(
                    dataMap.get("scheme"),
                    dataMap.get("host"),
                    dataMap.get("port"),
                    dataMap.get("path"),
                    dataMap.get("pathPrefix"),
                    dataMap.get("pathSuffix"),
                    dataMap.get("pathPattern"),
                    dataMap.get("pathAdvancedPattern"),
                    dataMap.get("mimeType")).convertToDataSet();
        }

        private boolean isEnabled(AXmlNode intentFilter) {
            return intentFilter.getParent().getAttribute("enabled") == null ||
                    intentFilter.getParent().getAttribute("enabled").getValue().equals(Boolean.TRUE);
        }

        private String getParentName(AXmlNode intentFilter) {
            return intentFilter.getParent().getAttribute("name") == null ? null : intentFilter.getParent().getAttribute("name").getValue().toString();
        }

        private MultiMap<String, JClass> getLayoutComponents(List<JClass> components, soot.util.MultiMap<String, String> map) {
            MultiMap<String, JClass> layoutComponents = Maps.newMultiMap();
            map.putAll(rawApkInfo.layoutFile().getFragmentsOrViews());
            map.forEach(pair -> {
                        JClass component = hierarchy.getClass(pair.getO2());
                        if (component != null && isSubClass(components, component)) {
                            layoutComponents.put(pair.getO1(), component);
                        }
                    }
            );
            return layoutComponents;
        }

        private boolean isSubClass(List<JClass> components, JClass c) {
            return components.stream()
                    .anyMatch(component -> hierarchy.isSubclass(component, c));
        }

        private Set<String> loadAndroidCallbacks() {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JavaType type = mapper.getTypeFactory()
                    .constructCollectionType(Set.class, String.class);
            try {
                InputStream content = AbstractWorldBuilder.class
                        .getClassLoader()
                        .getResourceAsStream(ANDROID_CALLBACKS);
                return mapper.readValue(content, type);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read callbackFile", e);
            }
        }

    }

}
