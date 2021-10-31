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

package pascal.taie.analysis.pta.core.cs.context;

import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;

/**
 * List-based contexts. Each context is represented by a list of context elements.
 *
 * @param <T> type of context elements
 */
public class ListContext<T> implements Context {

    /**
     * The empty context.
     */
    private static final ListContext<?> EMPTY_CONTEXT = new ListContext<>(List.of());

    /**
     * Cache for contexts with one element.
     */
    private static final Map<Object, ListContext<?>> oneContexts = Maps.newMap();

    /**
     * List of elements in the context.
     */
    private final List<T> elements;

    private ListContext(List<T> elements) {
        this.elements = elements;
    }

    /**
     * @return an empty context.
     */
    public static Context make() {
        return EMPTY_CONTEXT;
    }

    /**
     * @return a context that consists of the given context element.
     */
    public static <T> Context make(T e) {
        return oneContexts.computeIfAbsent(e,
                el -> new ListContext<>(List.of(el)));
    }

    /**
     * @return a context that consists of given context elements.
     */
    @SafeVarargs
    public static <T> Context make(T... elements) {
        switch (elements.length) {
            case 0: return make();
            case 1: return make(elements[0]);
            default: return new ListContext<>(List.of(elements));
        }
    }

    @Override
    public int getLength() {
        return elements.size();
    }

    @Override
    public Object getElementAt(int i) {
        if (i >= elements.size()) {
            throw new AnalysisException(
                    "Context " + this + " doesn't have " + i + "-th element");
        }
        return elements.get(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListContext<?> that = (ListContext<?>) o;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
