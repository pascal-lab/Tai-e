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

package pascal.taie.analysis.pta.plugin.android.icc;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.ir.exp.InvokeExp;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects {@link IntentAttribute} facts from invoke arguments.
 */
final class IntentAttributeCollector {

    private final Context context;

    private final CSManager csManager;

    private final InvokeExp invokeExp;

    private final List<IntentAttribute> attributes = new ArrayList<>();

    IntentAttributeCollector(Context context, CSManager csManager, InvokeExp invokeExp) {
        this.context = context;
        this.csManager = csManager;
        this.invokeExp = invokeExp;
    }

    CSVar arg(int index) {
        return csManager.getCSVar(context, invokeExp.getArg(index));
    }

    void addSingle(int argIndex, IntentAttributeKind kind) {
        attributes.add(new IntentAttribute(List.of(arg(argIndex)), kind));
    }

    void addPair(int firstArgIndex, int secondArgIndex, IntentAttributeKind kind) {
        attributes.add(new IntentAttribute(
                List.of(arg(firstArgIndex), arg(secondArgIndex)),
                kind));
    }

    List<IntentAttribute> attributes() {
        return attributes;
    }
}
