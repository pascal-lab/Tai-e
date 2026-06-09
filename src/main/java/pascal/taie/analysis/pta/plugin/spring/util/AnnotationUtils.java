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

package pascal.taie.analysis.pta.plugin.spring.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.StringElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AnnotationUtils {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationUtils.class);

    private static final String DEFAULT_KEY = "value";

    private AnnotationUtils() {
    }

    @Nullable
    public static String getStringElement(@Nullable Annotation anno) {
        return getStringElement(anno, DEFAULT_KEY);
    }

    @Nullable
    public static String getStringElement(@Nullable Annotation anno,
                                          String elementKey) {
        return Optional.ofNullable(anno)
                       .map(annotation -> annotation.getElement(elementKey))
                       .filter(ele -> ele instanceof StringElement)
                       .map(strEle -> ((StringElement) strEle).value())
                       .filter(value -> !value.isEmpty())
                       .orElse(null);
    }

    @Nullable
    public static String getStringElementAlias(@Nullable Annotation anno,
                                               String elementKey1,
                                               String elementKey2) {
        return Optional.ofNullable(getStringElement(anno, elementKey1))
                       .orElseGet(() -> getStringElement(anno, elementKey2));
    }

    @Nonnull
    public static List<String> getStringArrayElement(@Nullable Annotation anno,
                                                     String elementKey) {
        return Optional.ofNullable(anno)
                       .map(annotation -> annotation.getElement(elementKey))
                       .filter(ele -> ele instanceof ArrayElement)
                       .map(ele -> ((ArrayElement) ele).elements())
                       .orElseGet(List::of)
                       .stream()
                       .filter(ele -> ele instanceof StringElement)
                       .map(ele -> ((StringElement) ele).value())
                       .collect(Collectors.toList());
    }

    @Nonnull
    public static List<String> getStringArrayElementAlias(Annotation anno, String key1, String key2) {
        List<String> result1 = getStringArrayElement(anno, key1);
        List<String> result2 = getStringArrayElement(anno, key2);
        if ((result1.isEmpty() ^ result2.isEmpty()) || result1.equals(result2)) {
            return result1.isEmpty() ? result2 : result1;
        } else {
            logger.warn("Annotation '{}' has different content ('{}' and '{}') in alias key '{}' and '{}'",
                    anno, result1, result2, key1, key2);
        }
        return List.of();
    }

}
