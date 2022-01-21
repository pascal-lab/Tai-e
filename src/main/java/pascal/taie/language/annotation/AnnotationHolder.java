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

public class AnnotationHolder implements Annotated {

    private final Map<String, Annotation> annotations;

    public AnnotationHolder(Map<String, Annotation> annotations) {
        this.annotations = Map.copyOf(annotations);
    }

    @Override
    public boolean hasAnnotation(String annotationType) {
        return annotations.containsKey(annotationType);
    }

    @Override
    public @Nullable Annotation getAnnotation(String annotationType) {
        return annotations.get(annotationType);
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return annotations.values();
    }
}
