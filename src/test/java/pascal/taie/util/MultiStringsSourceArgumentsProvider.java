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


package pascal.taie.util;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

/**
 * @see MultiStringsSource
 */
class MultiStringsSourceArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(
            ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        int paramCount = method.getParameterCount();
        return context.getElement()
                .map(annotatedElement -> AnnotationSupport.findRepeatableAnnotations(
                        annotatedElement, MultiStringsSource.class))
                .map(List::stream)
                .map(parameterStream -> parameterStream.map(anno ->
                        Arguments.of((Object[]) expand(anno.value(), paramCount))))
                .orElse(Stream.empty());
    }

    private static String[] expand(String[] src, int length) {
        String[] dest;
        if (src.length > length) {
            throw new IllegalArgumentException(
                    String.format("Argument count mismatch: @MultiStringsValueSource provides %d arguments, "
                            + "but there are only %d parameters", src.length, length));
        } else if (src.length == length) {
            dest = src;
        } else {
            dest = new String[length];
            System.arraycopy(src, 0, dest, 0, src.length);
        }
        return dest;
    }

}
