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

package pascal.taie.analysis.oldpta.core.ci;

import pascal.taie.analysis.oldpta.ir.Obj;
import pascal.taie.language.classes.JField;

/**
 * Represents instance field nodes in PFG.
 */
public class InstanceField extends Pointer {

    private final Obj base;

    private final JField field;

    InstanceField(Obj base, JField field) {
        this.base = base;
        this.field = field;
    }

    Obj getBase() {
        return base;
    }

    JField getField() {
        return field;
    }

    @Override
    public String toString() {
        return base + "." + field.getName();
    }
}
