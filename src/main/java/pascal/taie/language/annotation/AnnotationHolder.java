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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Container of annotations.
 * This class makes it easy for a class to implement {@link Annotated}.
 */
public class AnnotationHolder {

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

    public @Nullable Annotation getAnnotation(String annotationType) {
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
