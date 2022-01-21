/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.annotation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationHolder {

    private final Map<String, Annotation> annotations;

    protected AnnotationHolder(Collection<Annotation> annotations) {
        this.annotations = annotations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Annotation::getType, a -> a));
    }

    public boolean hasAnnotation(String annotationType) {
        return annotations.containsKey(annotationType);
    }

    public @Nullable Annotation getAnnotation(String annotationType) {
        return annotations.get(annotationType);
    }

    public Collection<Annotation> getAnnotations() {
        return annotations.values();
    }

    private static final AnnotationHolder EMPTY_HOLDER = new AnnotationHolder(List.of());

    public static AnnotationHolder make(Collection<Annotation> map) {
        return map.isEmpty() ? EMPTY_HOLDER : new AnnotationHolder(map);
    }

    public static AnnotationHolder make() {
        return EMPTY_HOLDER;
    }
}
