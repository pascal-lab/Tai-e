/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.env;

import panda.pta.core.ProgramManager;
import panda.pta.element.Type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the string constants in the program.
 * The creation of string constants is actually controlled by runtime
 * environment, so we put them in this package.
 */
class StringConstantPool {

    private final Type STRING;
    private final ConcurrentMap<String, StringConstant> constants
            = new ConcurrentHashMap<>();

    StringConstantPool(ProgramManager pm) {
        STRING = pm.getUniqueTypeByName("java.lang.String");
    }

    StringConstant getStringConstant(String constant) {
        return constants.computeIfAbsent(constant,
                c -> new StringConstant(STRING, c));
    }
}
