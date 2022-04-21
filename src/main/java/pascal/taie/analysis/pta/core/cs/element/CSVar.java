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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.Type;

/**
 * Represents context-sensitive variables.
 */
public class CSVar extends AbstractPointer implements CSElement {

    private final Var var;

    private final Context context;

    CSVar(Var var, Context context, int index) {
        super(index);
        this.var = var;
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    /**
     * @return the variable (without context).
     */
    public Var getVar() {
        return var;
    }

    @Override
    public Type getType() {
        return var.getType();
    }

    @Override
    public String toString() {
        return context + ":" + var.getMethod() + "/" + var.getName();
    }
}
