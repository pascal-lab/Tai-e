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

package pascal.taie.ir;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.Indexer;

/**
 * Indexer for variables in a method (ir).
 */
public record LocalVarIndexer(IR ir) implements Indexer<Var> {

    @Override
    public int getIndex(Var var) {
        return var.getIndex();
    }

    @Override
    public Var getObject(int index) {
        return ir.getVar(index);
    }
}
