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

import java.util.Collections;
import java.util.Set;

/**
 * This {@link ParamProvider} returns this/parameter objects specified via its builder.
 * For non-specified this variable or parameters,
 * the {@link Builder#delegate}'s result will be returned.
 */
public class SpecifiedParamProvider implements ParamProvider {

    private final Set<Obj> thisObjs;

    private final Set<Obj>[] paramObjs;

    private SpecifiedParamProvider(Set<Obj> thisObjs, Set<Obj>[] paramObjs) {
        this.thisObjs = thisObjs;
        this.paramObjs = paramObjs;
    }

    @Override
    public Set<Obj> getThisObjs() {
        return thisObjs;
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        return paramObjs[i];
    }

    // TODO: validate input this/param objects?
    public static class Builder {

        private ParamProvider delegate;

        private final JMethod method;

        private Set<Obj> thisObjs;

        private final Set<Obj>[] paramObjs;

        @SuppressWarnings("unchecked")
        public Builder(JMethod method) {
            this.delegate = EmptyParamProvider.get();
            this.method = method;
            this.paramObjs = (Set<Obj>[]) new Set[method.getParamCount()];
        }

        public Builder setDelegate(ParamProvider delegate) {
            this.delegate = delegate;
            return this;
        }

        public Builder addThisObj(Obj thisObj) {
            if (thisObjs == null) {
                thisObjs = Sets.newHybridSet();
            }
            thisObjs.add(thisObj);
            return this;
        }

        public Builder addParamObj(int i, Obj paramObj) {
            if (paramObjs[i] == null) {
                paramObjs[i] = Sets.newHybridSet();
            }
            paramObjs[i].add(paramObj);
            return this;
        }

        public SpecifiedParamProvider build() {
            if (thisObjs == null) {
                thisObjs = delegate.getThisObjs();
            }
            thisObjs = Collections.unmodifiableSet(thisObjs);
            for (int i = 0; i < method.getParamCount(); ++i) {
                if (paramObjs[i] == null) {
                    paramObjs[i] = delegate.getParamObjs(i);
                }
                paramObjs[i] = Collections.unmodifiableSet(paramObjs[i]);
            }
            return new SpecifiedParamProvider(thisObjs, paramObjs);
        }
    }
}
