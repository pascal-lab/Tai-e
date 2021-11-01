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
 * Represents static field pointers.
 */
public class StaticField extends AbstractPointer {

    private final JField field;

    StaticField(JField field) {
        this.field = field;
    }

    /**
     * @return the corresponding static field of the StaticField pointer.
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
        return field.toString();
    }
}
