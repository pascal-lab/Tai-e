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

package bamboo.pta.statement;

public interface Statement {

    enum Kind {
        ALLOCATION,
        ASSIGN,
        ASSIGN_CAST,

        INSTANCE_LOAD,
        INSTANCE_STORE,

        ARRAY_LOAD,
        ARRAY_STORE,

        STATIC_LOAD,
        STATIC_STORE,

        CALL,
    }

    Kind getKind();
}
