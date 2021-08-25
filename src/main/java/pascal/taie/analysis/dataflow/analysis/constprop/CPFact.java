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

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.analysis.dataflow.fact.MapFact;
import pascal.taie.ir.exp.Var;

import java.util.Collections;
import java.util.Map;

public class CPFact extends MapFact<Var, Value> {

    CPFact(Map<Var, Value> map) {
        super(map);
    }

    CPFact() {
        this(Collections.emptyMap());
    }

    @Override
    public Value get(Var key) {
        return map.getOrDefault(key, Value.getUndef());
    }

    @Override
    public MapFact<Var, Value> copy() {
        return new CPFact(this.map);
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
