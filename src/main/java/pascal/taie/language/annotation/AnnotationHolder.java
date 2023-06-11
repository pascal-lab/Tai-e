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

package pascal.taie.language.annotation;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Container of annotations.
 * This class makes it easy for a class to implement {@link Annotated}.
 */
public class AnnotationHolder implements Serializable {

    /**
     * Map from annotation type to corresponding annotation in this holder.
     */
    private final Map<String, Annotation> annotations;

    protected AnnotationHolder(Collection<Annotation> annotations) {
        this.annotations = annotations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Annotation::getType, a -> a));
    }

    public boolean hasAnnotation(String annotationType) {
        return annotations.containsKey(annotationType);
    }

    @Nullable
    public Annotation getAnnotation(String annotationType) {
        return annotations.get(annotationType);
    }

    public Collection<Annotation> getAnnotations() {
        return annotations.values();
    }

    private static final AnnotationHolder EMPTY_HOLDER = new AnnotationHolder(Set.of());

    /**
     * Creates an annotation holder that contains the annotations
     * in given collection.
     */
    public static AnnotationHolder make(Collection<Annotation> annotations) {
        return annotations.isEmpty() ? EMPTY_HOLDER : new AnnotationHolder(annotations);
    }

    /**
     * @return an annotation holder that contains no annotations.
     */
    public static AnnotationHolder emptyHolder() {
        return EMPTY_HOLDER;
    }
}
