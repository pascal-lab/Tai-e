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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.Type;

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
        return ((ArrayType) array.getObject().getType())
                .getElementType();
    }

    @Override
    public String toString() {
        return array + "[*]";
    }
}
