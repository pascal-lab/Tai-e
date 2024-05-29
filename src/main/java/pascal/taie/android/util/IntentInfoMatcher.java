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
import pascal.taie.android.info.UriData;
import pascal.taie.android.info.TransferFilterInfo;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Intent-Filter matcher.
 */
public class IntentInfoMatcher {

    private final MultiMap<JClass, TransferFilterInfo> componentFilterInfo;

    private final Set<String> enabledComponents;

    public IntentInfoMatcher(ApkInfo apkInfo) {
        this.componentFilterInfo = apkInfo.componentFilterInfo();
        this.enabledComponents = apkInfo.getEnabledComponents()
                .stream()
                .map(JClass::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getMatchResult(TransferFilterInfo userFilterInfo, Set<String> matchDynamicReceiver) {
        Set<String> classNames = userFilterInfo.classNames().stream().filter(enabledComponents::contains).collect(Collectors.toSet());
        Set<String> intentFilterResult = classNames.isEmpty() ? matchIntentFilter(userFilterInfo) : Sets.newSet();
        return Stream.of(classNames,
                        intentFilterResult,
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

    private Set<String> matchIntentFilter(TransferFilterInfo userFilterInfo) {
        return componentFilterInfo.entrySet().stream()
                .filter(entry -> matchIntentFilter(entry.getValue(), userFilterInfo))
                .map(entry -> entry.getKey().getName())
                .collect(Collectors.toSet());
    }

    public boolean matchIntentFilter(TransferFilterInfo intentFilterInfo, TransferFilterInfo userFilterInfo) {
        return !userFilterInfo.emptyImplicitFilterInfo()
                && hasAction(intentFilterInfo.actions(), userFilterInfo.actions())
                && hasCategory(intentFilterInfo.categories(), userFilterInfo.categories())
                && hasData(intentFilterInfo.data(), userFilterInfo.data());
    }

    private boolean hasAction(Set<String> intentFilterActions, Set<String> userFilterActions) {
        return hasFilter(intentFilterActions, userFilterActions, this::actionMatch);
    }

    private boolean hasCategory(Set<String> intentFilterCategories, Set<String> categories) {
        return hasFilter(intentFilterCategories, categories, Set::containsAll);
    }

    private boolean hasData(Set<UriData> intentFilterData, Set<UriData> userFilterData) {
        return hasFilter(intentFilterData, userFilterData, this::dataMatch);
    }

    private <T> boolean hasFilter(Set<T> intentFilter, Set<T> userFilter, BiPredicate<Set<T>, Set<T>> predicate) {
        return predicate.test(intentFilter, userFilter);
    }

    private boolean actionMatch(Set<String> intentFilterActions, Set<String> userFilterActions) {
        return !intentFilterActions.isEmpty() && (userFilterActions.stream().anyMatch(intentFilterActions::contains)
                || userFilterActions.isEmpty());
    }

    private boolean dataMatch(Set<UriData> intentFilterData, Set<UriData> userFilterData) {
        return userFilterData.stream().anyMatch(d -> dataMatch(d, intentFilterData)) || (userFilterData.isEmpty() && intentFilterData.isEmpty());
    }

    private boolean dataMatch(UriData uriData, Set<UriData> intentFilterData) {
        return !isInvalidData(uriData) && intentFilterData.stream().anyMatch(baseData -> baseData.match(uriData));
    }

    private boolean isInvalidData(UriData uriData) {
        return (uriData.scheme() == null || uriData.host() == null) && uriData.mimeType() == null;
    }

    /**
     * refer Android library code
     */
    public static String normalizeScheme(String scheme) {
        return scheme.toLowerCase(Locale.ROOT);
    }

    /**
     * refer Android library code
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
