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

/**
 * Pointer analysis using old PTA IR. This package will be deprecated.
 * We keep it for now as it is depended by some interprocedural analyses,
 * and it will be removed after we rewrite the relevant analyses.
 * The user should use pointer analysis in {@link pascal.taie.analysis.pta}.
 */
@Deprecated
package pascal.taie.analysis.oldpta;
