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

package pascal.taie.frontend.newfrontend.bcir;

import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.Set;

class StackPhi implements Exp {

    /**
     * Same order as inBlocks.
     */
    private final List<StackItem> nodes;

    private Var var;
    private Var writeOutVar;
    private final int height;
    boolean used;
    BytecodeBlock createPos;

    boolean resolved = false;

    StackPhi(int i, List<StackItem> exps, BytecodeBlock block) {
        this.nodes = exps;
        this.height = i;
        this.createPos = block;
        used = false;
    }

    void setVar(Var var) {
        this.var = var;
    }

    void setUsed() {
        this.used = true;
    }

    Var getVar() {
        return this.var;
    }

    void setWriteOutVar(Var var) {
        assert writeOutVar == null;
        this.writeOutVar = var;
    }

    Var getWriteOutVar() {
        return this.writeOutVar;
    }

    List<StackItem> getNodes() {
        return nodes;
    }

    @Override
    public Type getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<RValue> getUses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }
}
