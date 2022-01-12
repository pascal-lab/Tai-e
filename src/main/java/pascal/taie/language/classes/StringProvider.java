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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marker annotation for the classes that provide string representations
 * of special program elements (e.g., names and signatures) via final fields.
 */
@Target(ElementType.TYPE)
@interface StringProvider {
}
