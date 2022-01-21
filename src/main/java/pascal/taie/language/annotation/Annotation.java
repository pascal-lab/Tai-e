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
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public class Annotation {

    private final RetentionPolicy retention;

    private final String annotationType;

    private final Map<String, Element> elements;

    public Annotation(RetentionPolicy retention,
                      String annotationType,
                      Map<String, Element> elements) {
        this.retention = retention;
        this.annotationType = annotationType;
        this.elements = Map.copyOf(elements);
    }

    public RetentionPolicy getRetention() {
        return retention;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public boolean hasElement(String key) {
        return elements.containsKey(key);
    }

    @Nullable Element getElement(String key) {
        return elements.get(key);
    }
}
