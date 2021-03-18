/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.oldpta.env;

import pascal.taie.language.classes.StringReps;
import pascal.taie.language.types.Type;
import pascal.taie.language.types.TypeManager;

import java.util.concurrent.ConcurrentMap;

import static pascal.taie.util.CollectionUtils.newConcurrentMap;

/**
 * Manages the string constants in the program.
 * The creation of string constants is actually controlled by runtime
 * environment, so we put them in this package.
 */
class StringConstantPool {

    private final Type STRING;
    private final ConcurrentMap<String, StringConstant> constants
            = newConcurrentMap();

    StringConstantPool(TypeManager typeManager) {
        STRING = typeManager.getClassType(StringReps.STRING);
    }

    StringConstant getStringConstant(String constant) {
        return constants.computeIfAbsent(constant,
                c -> new StringConstant(STRING, c));
    }
}
