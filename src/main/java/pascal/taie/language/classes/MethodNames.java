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

package pascal.taie.language.classes;

/**
 * Provides names of special methods.
 */
@StringProvider
public final class MethodNames {

    public static final String INIT = "<init>";

    public static final String CLINIT = "<clinit>";

    // Suppresses default constructor, ensuring non-instantiability.
    private MethodNames() {
    }
}
