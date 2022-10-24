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

package pascal.taie.analysis.bugfinder.nullpointer;

import pascal.taie.analysis.dataflow.fact.MapFact;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;

import java.util.Collections;
import java.util.Map;

class IsNullFact extends MapFact<Var, IsNullValue> {

    public IsNullFact() {
        this(Collections.emptyMap());
    }

    private IsNullFact(Map<Var, IsNullValue> map) {
        super(map);
    }

    private boolean isValid = true;

    private IsNullConditionDecision decision = null;

    @Override
    public IsNullValue get(Var var) {
        return map.getOrDefault(var, IsNullValue.UNDEF);
    }

    @Override
    public boolean update(Var key, IsNullValue value) {
        if (key.getType() instanceof ClassType
                || key.getType() instanceof ArrayType) {
            return super.update(key, value);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public IsNullFact copy() {
        return new IsNullFact(this.map);
    }

    public IsNullConditionDecision getDecision() {
        return decision;
    }

    public void setDecision(IsNullConditionDecision decision) {
        this.decision = decision;
    }

    public void downgradeOnControlSplit() {
        map.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isNullOnSomePath())
                .forEach(entry -> entry.setValue(IsNullValue.NCP));
    }

    public void setInvalid() {
        map.clear();
        isValid = false;
    }

    public void setValid() {
        isValid = true;
    }

    public boolean isValid() {
        return isValid;
    }
}
