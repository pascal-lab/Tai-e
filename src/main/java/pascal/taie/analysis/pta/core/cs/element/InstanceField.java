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

import pascal.taie.language.classes.JField;
import pascal.taie.language.type.Type;

/**
 * Represents instance field pointers.
 */
public class InstanceField extends AbstractPointer {

    private final CSObj base;

    private final JField field;

    InstanceField(CSObj base, JField field) {
        this.base = base;
        this.field = field;
    }

    /**
     * @return the base object.
     */
    public CSObj getBase() {
        return base;
    }

    /**
     * @return the corresponding instance field of the InstanceField pointer.
     */
    public JField getField() {
        return field;
    }

    @Override
    public Type getType() {
        return field.getType();
    }

    @Override
    public String toString() {
        return base + "." + field.getName();
    }
}
