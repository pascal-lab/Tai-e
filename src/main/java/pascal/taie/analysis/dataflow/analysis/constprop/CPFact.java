/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.analysis.dataflow.fact.MapFact;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Var;

import java.util.Collections;
import java.util.Map;

/**
 * Represents data facts of constant propagation, which maps variables
 * to their lattice values.
 * <p>
 * For better performance, this implementation treats temporary constant
 * variables specially. These variables have two features:
 * <ul>
 *     <li>its value is associated with itself</li>
 *     <li>its value never change</li>
 * </ul>
 * So this map does not need to actually store the values of these variables:
 * the values must be constants and can be obtained from the variables themselves.
 * <p>
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
    public Value get(Var var) {
        if (var.isConst() &&
                var.getConstValue() instanceof IntLiteral i) {
            // for temporary constant variable, directly return
            // the associated value
            return Value.makeConstant(i.getValue());
        } else {
            return map.getOrDefault(var, Value.getUndef());
        }
    }

    @Override
    public boolean update(Var var, Value value) {
        if (var.isConst()) {
            // do not store temporary constant variables
            return false;
        } else if (value.isUndef()) {
            // if the client code sets variable key to UNDEF,
            // then we remove the variable from the CPFact
            // as we use absence to represent UNDEF.
            return remove(var) != null;
        } else {
            return super.update(var, value);
        }
    }

    @Override
    public CPFact copy() {
        return new CPFact(this.map);
    }
}
