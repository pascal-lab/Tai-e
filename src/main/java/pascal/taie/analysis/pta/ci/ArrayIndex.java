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

package pascal.taie.analysis.pta.ci;

import pascal.taie.analysis.pta.core.heap.Obj;

/**
 * Represents array index pointers in PFG.
 */
class ArrayIndex extends Pointer {

    private final Obj array;

    ArrayIndex(Obj array) {
        this.array = array;
    }

    public Obj getArray() {
        return array;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArrayIndex that = (ArrayIndex) o;
        return array.equals(that.array);
    }

    @Override
    public int hashCode() {
        return array.hashCode();
    }

    @Override
    public String toString() {
        return array + "[*]";
    }
}
