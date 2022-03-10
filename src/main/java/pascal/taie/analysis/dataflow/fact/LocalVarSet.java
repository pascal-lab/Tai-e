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

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.AbstractBitSet;

/**
 * Represents set of local variables in an {@link IR}.
 */
public class LocalVarSet extends AbstractBitSet<Var> {

    private final IR ir;

    public LocalVarSet(IR ir) {
        this.ir = ir;
    }

    public LocalVarSet(LocalVarSet set) {
        super(set);
        this.ir = set.ir;
    }

    @Override
    public LocalVarSet copy() {
        return new LocalVarSet(this);
    }

    @Override
    protected Object getContext() {
        return ir;
    }

    @Override
    protected int getIndex(Var var) throws IllegalArgumentException {
        return var.getIndex();
    }

    @Override
    protected Var getElement(int index) throws IllegalArgumentException {
        return ir.getVar(index);
    }
}
