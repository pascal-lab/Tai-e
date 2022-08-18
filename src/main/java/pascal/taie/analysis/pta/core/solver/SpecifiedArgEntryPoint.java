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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Set;

/**
 * This entry point returns specified objects. For non-specified this variable
 * or parameters, an empty set is returned.
 */
public class SpecifiedArgEntryPoint extends EntryPoint {

    private final Set<Obj> thisObjs;

    private final Set<Obj>[] params;

    private SpecifiedArgEntryPoint(
            JMethod method, Set<Obj> thisObjs, Set<Obj>[] params) {
        super(method);
        this.thisObjs = thisObjs;
        this.params = params;
    }

    @Override
    public Set<Obj> getThis() {
        return thisObjs;
    }

    @Override
    public Set<Obj> getParam(int i) {
        return params[i];
    }

    // TODO: validate added this/param objects?
    public static class Builder {

        private final JMethod method;

        private final Set<Obj> thisObjs = Sets.newHybridSet();

        private final Set<Obj>[] params;

        @SuppressWarnings("unchecked")
        public Builder(JMethod method) {
            this.method = method;
            this.params = (Set<Obj>[]) new Set[method.getParamCount()];
        }

        public Builder addThis(Obj thisObj) {
            thisObjs.add(thisObj);
            return this;
        }

        public Builder addParam(int i, Obj param) {
            if (params[i] == null) {
                params[i] = Sets.newHybridSet();
            }
            params[i].add(param);
            return this;
        }

        public SpecifiedArgEntryPoint build() {
            // fill empty set to non-specified parameters
            for (int i = 0; i < method.getParamCount(); ++i) {
                if (params[i] == null) {
                    params[i] = Set.of();
                }
            }
            return new SpecifiedArgEntryPoint(method, thisObjs, params);
        }
    }
}
