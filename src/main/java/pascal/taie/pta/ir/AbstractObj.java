/*
 * Tai-e - A Program Analysis Framework for Java
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

package pascal.taie.pta.ir;

import pascal.taie.pta.element.Type;

/**
 * All implementations of Obj should inherit this class.
 */
public abstract class AbstractObj implements Obj {

    /**
     * Type of this object.
     */
    protected final Type type;

    protected AbstractObj(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }
}
