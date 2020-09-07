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

package pascal.panda.pta.core.cs;

import pascal.panda.pta.element.Type;

public class ArrayIndex extends AbstractPointer {

    private final CSObj array;

    ArrayIndex(CSObj array) {
        this.array = array;
    }

    public CSObj getArray() {
        return array;
    }

    @Override
    public Type getType() {
        return array.getObject().getType().getElementType();
    }

    @Override
    public String toString() {
        return array.toString();
    }
}
