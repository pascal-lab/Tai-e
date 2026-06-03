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
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.android.AndroidClassNames.ANDROIDX_FRAGMENT;
import static pascal.taie.android.AndroidClassNames.FRAGMENT;
import static pascal.taie.android.AndroidClassNames.VIEW;

/**
 * Aggregates Android application information used by Tai-e's Android analyses.
 */
public record ApkInfo(@Nullable JClass application,
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
                      MultiMap<JClass, IntentFilterAttribute> componentFilterAttribute,
                      RawApkInfo rawApkInfo) {

    /**
     * @return all enabled manifest components together with the application class.
     */
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

    /**
     * Converts manifest/resources/layout information parsed by FlowDroid
     * to Tai-e's internal representation.
     */
    public static class ApkInfoConverter {

        private static final String ANDROID_CALLBACKS = "android/android-callbacks.yml";

        private final RawApkInfo rawApkInfo;

        private final ProcessManifest manifest;

        private final ClassHierarchy hierarchy;

        public ApkInfoConverter(RawApkInfo rawApkInfo, ClassHierarchy hierarchy) {
            this.rawApkInfo = rawApkInfo;
            this.manifest = rawApkInfo.manifest();
            this.hierarchy = hierarchy;
        }

        @Nullable
        public JClass convertApplication() {
            IAndroidApplication application = manifest.getApplication();
            if (application == null || !application.isEnabled()) {
                return null;
            }
            String applicationName = application.getName();
            return applicationName == null ? null :
                    hierarchy.getClass(manifest.expandClassName(applicationName));
        }

        public Set<JClass> convertExportedActivities() {
            return getComponentsByFilter(
                    manifest.getActivities().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertExportedServices() {
            return getComponentsByFilter(
                    manifest.getServices().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertExportedBroadcastReceivers() {
            return getComponentsByFilter(
                    manifest.getBroadcastReceivers().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertExportedContentProviders() {
            return getComponentsByFilter(
                    manifest.getContentProviders().asList(), IAndroidComponent::isExported);
        }

        public Set<JClass> convertEnabledActivities() {
            return getComponentsByFilter(
                    manifest.getActivities().asList(), IAndroidComponent::isEnabled);
        }

        public Set<JClass> convertEnabledServices() {
            return getComponentsByFilter(
                    manifest.getServices().asList(), IAndroidComponent::isEnabled);
        }

        public Set<JClass> convertEnabledBroadcastReceivers() {
            return getComponentsByFilter(
                    manifest.getBroadcastReceivers().asList(), IAndroidComponent::isEnabled);
        }

        public Set<JClass> convertEnabledContentProviders() {
            return getComponentsByFilter(
                    manifest.getContentProviders().asList(), IAndroidComponent::isEnabled);
        }

        private Set<JClass> getComponentsByFilter(List<? extends IAndroidComponent> components,
                                                  Predicate<IAndroidComponent> filterPredicate) {
            return components.stream()
                    .filter(filterPredicate)
                    .filter(c ->
                            c.getNameString() != null && hierarchy.getClass(c.getNameString()) != null)
                    .map(c -> hierarchy.getClass(c.getNameString()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        public MultiMap<String, Subsignature> convertLayoutCallbacks() {
            MultiMap<String, Subsignature> layoutCallbacks = Maps.newMultiMap();
            rawApkInfo.layoutFile().getCallbackMethods().forEach((key, value) ->
                    layoutCallbacks.put(key,
                            Subsignature.get("void " + value + "(" + VIEW + ")"))
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

        /**
         * Collects intent-filter information keyed by the declaring component.
         */
        public MultiMap<JClass, IntentFilterAttribute> convertComponentFilterAttribute() {
            return new ManifestIntentFilterResolver(rawApkInfo, manifest, hierarchy)
                    .convert();
        }

        /**
         * Layout parser records fragments/views in different buckets. This
         * method merges them and keeps only classes that are subclasses of the
         * requested Android base classes.
         */
        private MultiMap<String, JClass> getLayoutComponents(
                List<JClass> superClasses, MultiMap<String, String> componentNames) {
            MultiMap<String, JClass> layoutComponents = Maps.newMultiMap();
            addLayoutComponents(layoutComponents, superClasses, componentNames);
            addLayoutComponents(layoutComponents,
                    superClasses, rawApkInfo.layoutFile().getFragmentsOrViews());
            return layoutComponents;
        }

        private void addLayoutComponents(MultiMap<String, JClass> layoutComponents,
                                         List<JClass> superClasses,
                                         MultiMap<String, String> componentNames) {
            componentNames.forEach((key, value) -> {
                JClass component = hierarchy.getClass(value);
                if (component != null && isSubclass(component, superClasses)) {
                    layoutComponents.put(key, component);
                }
            });
        }

        private boolean isSubclass(JClass subclass, List<JClass> superClasses) {
            return superClasses.stream()
                    .anyMatch(superClass -> hierarchy.isSubclass(superClass, subclass));
        }

        /**
         * Loads Android callback interface/class names configured for lifecycle
         * callback discovery.
         */
        private Set<String> loadAndroidCallbacks() {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JavaType type = mapper.getTypeFactory()
                    .constructCollectionType(Set.class, String.class);
            try (InputStream content = ApkInfo.class
                    .getClassLoader()
                    .getResourceAsStream(ANDROID_CALLBACKS)) {
                if (content == null) {
                    throw new RuntimeException("Failed to find callback file: " + ANDROID_CALLBACKS);
                }
                return mapper.readValue(content, type);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read callback file: " + ANDROID_CALLBACKS, e);
            }
        }

    }

}
