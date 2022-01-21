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
import java.util.Map;

public class Annotation {

    private final String annotationType;

    private final Map<String, Element> elements;

    public Annotation(String annotationType,
                      Map<String, Element> elements) {
        this.annotationType = annotationType;
        this.elements = Map.copyOf(elements);
    }

    public String getType() {
        return annotationType;
    }

    public boolean hasElement(String name) {
        return elements.containsKey(name);
    }

    public @Nullable Element getElement(String name) {
        return elements.get(name);
    }
}
