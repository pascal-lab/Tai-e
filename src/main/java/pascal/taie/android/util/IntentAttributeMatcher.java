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

package pascal.taie.android.util;

import pascal.taie.android.info.ApkInfo;
import pascal.taie.android.info.IntentFilterAttribute;
import pascal.taie.android.info.UriData;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Matches an intent-like {@link IntentFilterAttribute} against manifest-declared
 * component intent filters.
 *
 * <p>The matcher returns component class names instead of {@link JClass} objects
 * because downstream ICC code records component targets by class name. Dynamic
 * receivers are passed in by callers and merged with the manifest match result.
 */
public class IntentAttributeMatcher {

    private final MultiMap<JClass, IntentFilterAttribute> filtersByComponent;

    private final Set<String> enabledComponents;

    public IntentAttributeMatcher(ApkInfo apkInfo) {
        this.filtersByComponent = apkInfo.componentFilterAttribute();
        this.enabledComponents = apkInfo.getEnabledComponents()
                .stream()
                .map(JClass::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all possible target component names for the given intent attribute.
     */
    public Set<String> getMatchResult(IntentFilterAttribute userFilterAttribute, Set<String> matchDynamicReceiver) {
        Set<String> explicitTargets = userFilterAttribute.classNames().stream()
                .filter(enabledComponents::contains).collect(Collectors.toSet());
        Set<String> implicitTargets = explicitTargets.isEmpty() ? matchManifestIntentFilter(userFilterAttribute) : Sets.newSet();
        return Stream.of(explicitTargets,
                        implicitTargets,
                        matchDynamicReceiver)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public Set<UriData> mergeData(Set<UriData> preData, Set<UriData> postType) {
        Set<UriData> data = Sets.newSet();
        for (UriData pre : preData) {
            for (UriData post : postType) {
                data.add(UriData.builder()
                        .data(pre)
                        .mimeType(post.mimeType())
                        .build());
            }
        }
        return data;
    }

    private Set<String> matchManifestIntentFilter(IntentFilterAttribute userFilterAttribute) {
        return filtersByComponent.entrySet().stream()
                .filter(entry -> matchIntentFilter(entry.getValue(), userFilterAttribute))
                .map(entry -> entry.getKey().getName())
                .collect(Collectors.toSet());
    }

    public boolean matchIntentFilter(IntentFilterAttribute intentFilterAttribute, IntentFilterAttribute userFilterAttribute) {
        return !userFilterAttribute.emptyAttribute()
                && matchesAction(intentFilterAttribute.actions(), userFilterAttribute.actions())
                && matchesCategories(intentFilterAttribute.categories(), userFilterAttribute.categories())
                && matchesData(intentFilterAttribute.data(), userFilterAttribute.data());
    }

    private boolean matchesAction(Set<String> intentFilterActions, Set<String> userFilterActions) {
        return !intentFilterActions.isEmpty()
                && (userFilterActions.stream().anyMatch(intentFilterActions::contains)
                || userFilterActions.isEmpty());
    }

    private boolean matchesCategories(Set<String> intentFilterCategories,
                                      Set<String> userCategories) {
        return intentFilterCategories.containsAll(userCategories);
    }

    private boolean matchesData(Set<UriData> intentFilterData, Set<UriData> userFilterData) {
        return userFilterData.stream().anyMatch(d ->
                matchesData(d, intentFilterData)) ||
                (userFilterData.isEmpty() && intentFilterData.isEmpty());
    }

    private boolean matchesData(UriData uriData, Set<UriData> intentFilterData) {
        return !isInvalidData(uriData)
                && intentFilterData.stream().anyMatch(baseData -> baseData.match(uriData));
    }

    private boolean isInvalidData(UriData uriData) {
        return (uriData.scheme() == null || uriData.host() == null) && uriData.mimeType() == null;
    }

    /**
     * Normalizes URI schemes following Android framework behavior.
     */
    public static String normalizeScheme(String scheme) {
        return scheme.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes MIME types following Android framework behavior.
     *
     * <p>Parameters after ';' are ignored, e.g. {@code text/plain; charset=utf-8}
     * is normalized to {@code text/plain}.
     */
    public static String normalizeMimeType(String mimeType) {
        mimeType = mimeType.trim().toLowerCase(Locale.ROOT);

        final int semicolonIndex = mimeType.indexOf(';');
        if (semicolonIndex != -1) {
            mimeType = mimeType.substring(0, semicolonIndex);
        }
        return mimeType;
    }

}
