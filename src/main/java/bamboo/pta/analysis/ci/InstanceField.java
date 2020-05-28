/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.ci;

import bamboo.pta.element.Field;
import bamboo.pta.element.Obj;

/**
 * Represents instance field nodes in PFG.
 */
public class InstanceField extends Pointer {

    private final Obj base;

    private final Field field;

    InstanceField(Obj base, Field field) {
        this.base = base;
        this.field = field;
    }

    Obj getBase() {
        return base;
    }

    Field getField() {
        return field;
    }

    @Override
    public String toString() {
        return base + "." + field;
    }
}
