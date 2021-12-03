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
 * This package contains implementation of Scaler, which selects
 * suitable context variants for the methods in the program.
 * <p>
 * The technique was presented in paper:
 * Yue Li, Tian Tan, Anders Møller, and Yannis Smaragdakis.
 * Scalability-First Pointer Analysis with Self-Tuning Context-Sensitivity.
 * In ESEC/FSE 2018.
 */
package pascal.taie.analysis.pta.toolkit.scaler;
