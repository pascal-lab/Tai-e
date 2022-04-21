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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Pair;

import java.util.List;
import java.util.stream.IntStream;

public class LookupSwitch extends SwitchStmt {

    private final List<Integer> caseValues;

    public LookupSwitch(Var var, List<Integer> caseValues) {
        super(var);
        this.caseValues = List.copyOf(caseValues);
    }

    public int getCaseValue(int index) {
        return caseValues.get(index);
    }

    @Override
    public List<Integer> getCaseValues() {
        return caseValues;
    }

    @Override
    public List<Pair<Integer, Stmt>> getCaseTargets() {
        return IntStream.range(0, caseValues.size())
                .mapToObj(i -> new Pair<>(caseValues.get(i),
                        targets == null ? null : targets.get(i)))
                .toList();
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getInsnString() {
        return "lookupswitch";
    }
}
