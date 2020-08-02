/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.env;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.Type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the string constants in the program.
 * The creation of string constants is actually controlled by runtime
 * environment, so we put them in this package.
 */
class StringConstantPool {

    private final ProgramManager pm;
    private final Map<String, StringConstant> constants =
            new ConcurrentHashMap<>();
    private Type stringType;

    StringConstantPool(ProgramManager pm) {
        this.pm = pm;
    }

    StringConstant getStringConstant(String constant) {
        return constants.computeIfAbsent(constant,
                c -> new StringConstant(getStringType(), c));
    }

    private Type getStringType() {
        if (stringType == null) {
            stringType = pm.getUniqueTypeByName("java.lang.String");
        }
        return stringType;
    }
}
