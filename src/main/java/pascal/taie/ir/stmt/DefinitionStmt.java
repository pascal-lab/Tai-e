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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;

import javax.annotation.Nullable;

public abstract class DefinitionStmt<L extends LValue, R extends RValue>
        extends AbstractStmt {

    public abstract @Nullable
    L getLValue();

    public abstract R getRValue();

    @Override
    public boolean canFallThrough() {
        return true;
    }
}
