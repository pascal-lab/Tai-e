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

import java.util.List;
import java.util.StringJoiner;

public record ArrayElement(List<Element> elements) implements Element {

    public ArrayElement(List<Element> elements) {
        this.elements = List.copyOf(elements);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "{", "}");
        elements.forEach(e -> sj.add(e.toString()));
        return sj.toString();
    }
}
