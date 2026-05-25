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

package pascal.taie.analysis.pta.plugin.android.icc;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttribute;
import pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind;
import pascal.taie.android.info.IntentDataInfo;
import pascal.taie.android.info.IntentFilterAttribute;
import pascal.taie.android.info.UriData;
import pascal.taie.android.util.IntentAttributeMatcher;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Sets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.android.util.IntentAttributeMatcher.normalizeMimeType;
import static pascal.taie.android.util.IntentAttributeMatcher.normalizeScheme;

/**
 * Resolves recorded Intent/IntentFilter facts to ICC target components.
 */
final class IntentTargetResolver {

    private static final String DEFAULT_CATEGORY = "android.intent.category.DEFAULT";

    private final ICCContext handlerContext;

    private final Solver solver;

    private final ClassHierarchy hierarchy;

    private final IntentAttributeMatcher intentAttributeMatcher;

    IntentTargetResolver(ICCContext handlerContext, Solver solver, ClassHierarchy hierarchy) {
        this.handlerContext = handlerContext;
        this.solver = solver;
        this.hierarchy = hierarchy;
        this.intentAttributeMatcher = new IntentAttributeMatcher(handlerContext.apkInfo());
    }

    Set<JClass> getTargetComponents(CSObj intentObj, boolean isStartActivity) {
        return getMatchResult(
                toIntentFilterAttribute(
                        handlerContext.intent2IntentAttribute().get(intentObj),
                        isStartActivity));
    }

    private IntentFilterAttribute toDynamicIntentFilterAttribute(Set<IntentAttribute> attributes) {
        IntentFilterAttribute baseFilter = toIntentFilterAttribute(attributes, false);
        Set<String> schemes = Sets.newSet();
        Set<String> hosts = Sets.newSet();
        Set<String> ports = Sets.newSet();
        Set<String> paths = Sets.newSet();
        Set<String> mimeTypes = Sets.newSet();
        for (IntentAttribute attribute : attributes) {
            switch (attribute.kind()) {
                case DATA_SCHEME -> schemes.addAll(toConstantStrings(attribute.csVar().get(0)));
                case DATA_HOST -> hosts.addAll(toConstantStrings(attribute.csVar().get(0)));
                case DATA_PORT -> ports.addAll(toConstantStrings(attribute.csVar().get(0)));
                case DATA_PATH -> paths.addAll(toConstantStrings(attribute.csVar().get(0)));
                case MIME_TYPE -> mimeTypes.addAll(toConstantStrings(attribute.csVar().get(0)));
            }
        }
        return new IntentFilterAttribute(
                baseFilter.classNames(),
                baseFilter.actions(),
                baseFilter.categories(),
                new IntentDataInfo(
                        schemes,
                        hosts,
                        ports,
                        paths,
                        Sets.newSet(),
                        Sets.newSet(),
                        Sets.newSet(),
                        Sets.newSet(),
                        mimeTypes).convertToDataSet());
    }

    private IntentFilterAttribute toIntentFilterAttribute(
            Set<IntentAttribute> attributes, boolean isStartActivity) {
        Set<String> classNames = Sets.newSet();
        Set<String> actions = Sets.newSet();
        Set<String> categories = Sets.newSet();
        Set<UriData> data = Sets.newSet();
        for (IntentAttribute attribute : attributes) {
            switch (attribute.kind()) {
                case CLASS -> classNames.addAll(toConstantStrings(attribute.csVar().get(0)));
                case COMPONENT_NAME -> classNames.addAll(toComponentNames(attribute.csVar().get(0)));
                case ACTION -> actions.addAll(toConstantStrings(attribute.csVar().get(0)));
                case CATEGORY -> categories.addAll(toConstantStrings(attribute.csVar().get(0)));
                case DATA, NORMALIZE_DATA, MIME_TYPE, NORMALIZE_MIME_TYPE ->
                        data.addAll(toUriData(attribute.csVar().get(0), attribute.kind()));
                // Data and MIME type are conjunctive Intent constraints, so merge them into one data set.
                case DATA_AND_MIME_TYPE ->
                        data.addAll(intentAttributeMatcher.mergeData(
                                toUriData(attribute.csVar().get(0), IntentAttributeKind.DATA),
                                toUriData(attribute.csVar().get(1), IntentAttributeKind.MIME_TYPE)));
                case NORMALIZE_DATA_AND_NORMALIZE_MIME_TYPE ->
                        data.addAll(intentAttributeMatcher.mergeData(
                                toUriData(attribute.csVar().get(0), IntentAttributeKind.NORMALIZE_DATA),
                                toUriData(attribute.csVar().get(1), IntentAttributeKind.NORMALIZE_MIME_TYPE)));
            }
        }

        if (isStartActivity) {
            categories.add(DEFAULT_CATEGORY);
        }
        return new IntentFilterAttribute(classNames, actions, categories, data);
    }

    private Set<JClass> getMatchResult(IntentFilterAttribute intentAttribute) {
        return intentAttributeMatcher.getMatchResult(
                        intentAttribute,
                        getDynamicReceiverMatches(intentAttribute))
                .stream()
                .map(hierarchy::getClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getDynamicReceiverMatches(IntentFilterAttribute intentAttribute) {
        return handlerContext.intentFiltersByDynamicReceiver()
                .entrySet()
                .stream()
                .flatMap(entry -> solver.getPointsToSetOf(entry.getValue()).getObjects().stream()
                        .filter(filterObj -> intentAttributeMatcher.matchIntentFilter(
                                toDynamicIntentFilterAttribute(
                                        handlerContext.intentFilter2Attribute().get(filterObj)),
                                intentAttribute))
                        .map(filterObj -> entry.getKey().getName()))
                .collect(Collectors.toSet());
    }

    private Set<String> toComponentNames(CSVar componentNameVar) {
        return solver.getPointsToSetOf(componentNameVar)
                .objects()
                .flatMap(componentNameObj ->
                        handlerContext.componentName2Info().get(componentNameObj).stream())
                .flatMap(componentClassVar -> toConstantStrings(componentClassVar).stream())
                .collect(Collectors.toSet());
    }

    private Set<String> toConstantStrings(CSVar csVar) {
        return solver.getPointsToSetOf(csVar)
                .objects()
                .map(CSObj::getObject)
                .map(Obj::getAllocation)
                .map(allocation -> {
                    if (allocation instanceof StringLiteral stringLiteral) {
                        return stringLiteral.getString();
                    } else if (allocation instanceof ClassLiteral classLiteral) {
                        return classLiteral.getTypeValue().getName();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<UriData> toUriData(CSVar csVar, IntentAttributeKind kind) {
        Set<UriData> data = Sets.newSet();
        // <scheme>://<host>:<port>[<path>|<pathPrefix>|<pathPattern>|<pathAdvancedPattern>|<pathSuffix>]
        toConstantStrings(csVar).forEach(rawValue -> {
            try {
                UriData uriData = null;
                if (kind == IntentAttributeKind.DATA || kind == IntentAttributeKind.NORMALIZE_DATA) {
                    URI uri = new URI(rawValue);
                    String scheme = uri.getScheme();
                    if (scheme != null && kind == IntentAttributeKind.NORMALIZE_DATA) {
                        scheme = normalizeScheme(scheme);
                    }
                    String host = uri.getHost() == null || uri.getHost().isEmpty()
                            ? null
                            : uri.getHost();
                    String port = uri.getPort() == -1 ? null : String.valueOf(uri.getPort());
                    String path = uri.getPath() == null || uri.getPath().isEmpty()
                            ? null
                            : uri.getPath();
                    uriData = UriData.builder()
                            .scheme(scheme)
                            .host(host)
                            .port(port)
                            .path(path)
                            .build();
                } else if (kind == IntentAttributeKind.MIME_TYPE
                        || kind == IntentAttributeKind.NORMALIZE_MIME_TYPE) {
                    String mimeType = kind == IntentAttributeKind.NORMALIZE_MIME_TYPE
                            ? normalizeMimeType(rawValue)
                            : rawValue;
                    uriData = UriData.builder()
                            .mimeType(mimeType)
                            .build();
                }

                if (uriData != null) {
                    data.add(uriData);
                }
            } catch (URISyntaxException ignored) {
            }
        });
        return data;
    }
}
