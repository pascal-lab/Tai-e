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

/**
 * Represents data facts of constant propagation, which maps variables
 * to their lattice values.
 *
 * Note that in this implementation, we use absence to represent UNDEF,
 * i.e., if a CPFact does not contain variable-value mapping of a variable,
 * it represents that the lattice value of the variable is UNDEF;
 * moreover, if we set the lattice value of a variable to UNDEF,
 * it effectively removes the variable from the CPFact.
 */
public class CPFact extends MapFact<Var, Value> {

    public CPFact() {
        this(Collections.emptyMap());
    }

    private CPFact(Map<Var, Value> map) {
        super(map);
    }

    /**
     * @return the value of given variable in this fact,
     * or UNDEF the variable is absent in this fact.
     */
    @Override
    public Value get(Var key) {
        return map.getOrDefault(key, Value.getUndef());
    }

    @Override
    public boolean update(Var key, Value value) {
        if (value.isUndef()) {
            // if the client code sets variable key to UNDEF,
            // then we remove the variable from the CPFact
            // as we use absence to represent UNDEF.
            return remove(key) != null;
        } else {
            return super.update(key, value);
        }
    }

    @Override
    public CPFact copy() {
        return new CPFact(this.map);
    }
}
