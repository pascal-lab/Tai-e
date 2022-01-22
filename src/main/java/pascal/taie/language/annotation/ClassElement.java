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

/**
 * Represents class constant elements.
 * We uses {@code String} instead of {@code Type} to represent the type
 * information of class element for the same reason as {@link Annotation}.
 */
public record ClassElement(String classDescriptor) implements Element {

    @Override
    public String toString() {
        return classDescriptor + ".class";
    }
}
