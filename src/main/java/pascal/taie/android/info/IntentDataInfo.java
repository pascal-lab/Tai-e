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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manifest-style intent data attributes before they are expanded into concrete
 * {@link UriData} combinations.
 */
public record IntentDataInfo(Set<String> schemes,
                             Set<String> hosts,
                             Set<String> ports,
                             Set<String> paths,
                             Set<String> pathPrefixes,
                             Set<String> pathSuffixes,
                             Set<String> pathPatterns,
                             Set<String> pathAdvancedPatterns,
                             Set<String> mimeTypes) {

    /**
     * Expands the configured attribute sets into concrete {@link UriData}
     * combinations used by intent-filter matching.
     */
    public Set<UriData> convertToDataSet() {
        Set<UriData> dataSet = Set.of();
        for (AttributeValues attributeValues : attributeValues()) {
            if (!attributeValues.values().isEmpty()) {
                dataSet = expandDataSet(dataSet, attributeValues);
            }
        }
        return dataSet;
    }

    private List<AttributeValues> attributeValues() {
        return List.of(
                new AttributeValues(DataAttribute.SCHEME, schemes),
                new AttributeValues(DataAttribute.HOST, hosts),
                new AttributeValues(DataAttribute.PORT, ports),
                new AttributeValues(DataAttribute.PATH, paths),
                new AttributeValues(DataAttribute.PATH_PREFIX, pathPrefixes),
                new AttributeValues(DataAttribute.PATH_SUFFIX, pathSuffixes),
                new AttributeValues(DataAttribute.PATH_PATTERN, pathPatterns),
                new AttributeValues(DataAttribute.PATH_ADVANCED_PATTERN, pathAdvancedPatterns),
                new AttributeValues(DataAttribute.MIME_TYPE, mimeTypes));
    }

    private static Set<UriData> expandDataSet(Set<UriData> dataSet,
                                              AttributeValues attributeValues) {
        Set<UriData> baseDataSet = dataSet.isEmpty() ?
                Set.of(UriData.builder().build()) :
                dataSet;
        return baseDataSet
                .stream()
                .flatMap(data -> attributeValues.values()
                        .stream()
                        .map(value -> attributeValues.attribute().withValue(data, value)))
                .collect(Collectors.toSet());
    }

    private record AttributeValues(DataAttribute attribute, Set<String> values) {
    }

    private enum DataAttribute {

        SCHEME {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).scheme(value).build();
            }
        },
        HOST {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).host(value).build();
            }
        },
        PORT {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).port(value).build();
            }
        },
        PATH {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).path(value).build();
            }
        },
        PATH_PREFIX {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).pathPrefix(value).build();
            }
        },
        PATH_SUFFIX {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).pathSuffix(value).build();
            }
        },
        PATH_PATTERN {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).pathPattern(value).build();
            }
        },
        PATH_ADVANCED_PATTERN {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).pathAdvancedPattern(value).build();
            }
        },
        MIME_TYPE {
            @Override
            UriData withValue(UriData data, String value) {
                return UriData.builder().data(data).mimeType(value).build();
            }
        };

        abstract UriData withValue(UriData data, String value);
    }

}
