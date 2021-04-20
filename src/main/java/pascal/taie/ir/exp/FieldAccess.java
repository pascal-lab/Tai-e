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

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.type.Type;

/**
 * Representation of field access expressions.
 */
public abstract class FieldAccess implements LValue, RValue {

    protected final FieldRef fieldRef;

    protected FieldAccess(FieldRef fieldRef) {
        this.fieldRef = fieldRef;
    }

    public FieldRef getFieldRef() {
        return fieldRef;
    }

    @Override
    public Type getType() {
        return fieldRef.getType();
    }
}
