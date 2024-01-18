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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.function.Predicate;

/**
 * Handles sanitizers in taint analysis.
 */
class SanitizerHandler extends OnFlyHandler {

    private final MultiMap<JMethod, ParamSanitizer> paramSanitizers = Maps.newMultiMap();

    /**
     * Used to filter out taint objects from points-to set.
     */
    private final Predicate<CSObj> taintFilter;

    SanitizerHandler(HandlerContext context) {
        super(context);
        taintFilter = o -> !context.manager().isTaint(o.getObject());
        context.config().paramSanitizers()
                .forEach(s -> this.paramSanitizers.put(s.method(), s));
    }

    /**
     *
     * Handles parameter sanitizers.
     */
    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (paramSanitizers.containsKey(method)) {
            Context context = csMethod.getContext();
            IR ir = method.getIR();
            paramSanitizers.get(method).forEach(sanitizer -> {
                int index = sanitizer.index();
                Var param = ir.getParam(index);
                CSVar csParam = csManager.getCSVar(context, param);
                solver.addPointerFilter(csParam, taintFilter);
            });
        }
    }
}
