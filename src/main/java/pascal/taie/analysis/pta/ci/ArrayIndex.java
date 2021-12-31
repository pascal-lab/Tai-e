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
 * <p>
 * Ideally, an array index should consist of an array object and an index.
 * However, pointer analysis does not distinguish loads and stores to
 * different indexes of an array, and treats arrays as special objects
 * with a mock field. Since there is only one such mock field of each array,
 * we don't need to represent the field explicitly.
 */
class ArrayIndex extends Pointer {

    private final Obj array;

    ArrayIndex(Obj array) {
        this.array = array;
    }

    /**
     * @return the array object.
     */
    Obj getArray() {
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
