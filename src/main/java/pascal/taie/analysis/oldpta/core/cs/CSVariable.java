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

package pascal.taie.analysis.oldpta.core.cs;

import pascal.taie.analysis.oldpta.core.context.Context;
import pascal.taie.analysis.oldpta.ir.Variable;
import pascal.taie.language.type.Type;

public class CSVariable extends AbstractPointer implements CSElement {

    private final Variable var;

    private final Context context;

    CSVariable(Variable var, Context context) {
        this.var = var;
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public Variable getVariable() {
        return var;
    }

    @Override
    public Type getType() {
        return var.getType();
    }

    @Override
    public String toString() {
        return context + ":" + var;
    }
}
