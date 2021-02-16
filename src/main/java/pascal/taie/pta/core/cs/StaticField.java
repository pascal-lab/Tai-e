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

package pascal.taie.pta.core.cs;

import pascal.taie.java.classes.JField;
import pascal.taie.java.types.Type;

public class StaticField extends AbstractPointer {

    private final JField field;

    StaticField(JField field) {
        this.field = field;
    }

    public JField getField() {
        return field;
    }

    @Override
    public Type getType() {
        return field.getType();
    }

    @Override
    public String toString() {
        return field.toString();
    }
}
