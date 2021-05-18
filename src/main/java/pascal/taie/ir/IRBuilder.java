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

package pascal.taie.ir;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;

/**
 * Interface for builder of {@link IR}.
 */
public interface IRBuilder {

    /**
     * Builds IR for concrete methods.
     */
    IR buildIR(JMethod method);

    /**
     * Builds IR for all methods in the given hierarchy.
     */
    void buildAll(ClassHierarchy hierarchy);
}
