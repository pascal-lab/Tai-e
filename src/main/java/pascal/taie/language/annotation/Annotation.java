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

import pascal.taie.util.collection.Views;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Represents annotations in the program.
 * Each annotation contains 0 or more named elements.
 *
 * Currently, we use {@code String} (instead of {@code JClass}
 * or {@code ClassType}) to represent the type of an annotation.
 * This makes it easier for the frontends to extract annotations
 * from the program (The type string is ready in the program,
 * and the frontends do not need to resolve the string to {@code JClass}).
 *
 * TODO: add ElementType and RetentionPolicy.
 */
public class Annotation {

    /**
     * String representation of type of this annotation.
     */
    private final String annotationType;

    /**
     * Map from names to corresponding elements in this annotation.
     */
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

    /**
     * @return all name-element entries in this annotation.
     */
    public Set<Entry> getElementEntries() {
        return Views.toMappedSet(elements.entrySet(),
                e -> new Entry(e.getKey(), e.getValue()));
    }

    /**
     * Represents name-element entries in annotations.
     */
    public record Entry(String name, Element element) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("@");
        sb.append(annotationType);
        int elems = elements.size();
        if (elems > 0) {
            sb.append('(');
            if (elems == 1 && elements.containsKey("value")) {
                // single-element annotation
                sb.append(elements.get("value"));
            } else {
                // normal annotation
                int count = 0;
                for (var e : elements.entrySet()) {
                    sb.append(e.getKey()).append('=').append(e.getValue());
                    if (++count < elems) {
                        sb.append(',');
                    }
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
