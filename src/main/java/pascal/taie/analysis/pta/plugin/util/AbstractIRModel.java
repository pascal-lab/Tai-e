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

package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Provides common functionalities for implementing IR-based API models.
 */
public abstract class AbstractIRModel implements IRModel {

    protected final Solver solver;

    protected final ClassHierarchy hierarchy;

    protected final TypeSystem typeSystem;

    protected final Map<JMethod, Function<Invoke, Collection<Stmt>>> api2IRGen
            = Maps.newHybridMap();

    protected final Map<JMethod, Collection<Stmt>> method2GenStmts
            = Maps.newHybridMap();

    protected AbstractIRModel(Solver solver) {
        this.solver = solver;
        this.hierarchy = solver.getHierarchy();
        this.typeSystem = solver.getTypeSystem();
        registerIRGens();
    }

    protected abstract void registerIRGens();

    protected void registerIRGen(JMethod api, Function<Invoke, Collection<Stmt>> irGen) {
        api2IRGen.put(api, irGen);
    }

    @Override
    public Set<JMethod> getModeledAPIs() {
        return api2IRGen.keySet();
    }

    @Override
    public void handleNewMethod(JMethod method) {
        List<Stmt> stmts = new ArrayList<>();
        method.getIR().invokes(false).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolveNullable();
            var irGen = api2IRGen.get(target);
            if (irGen != null) {
                stmts.addAll(irGen.apply(invoke));
            }
        });
        if (!stmts.isEmpty()) {
            method2GenStmts.put(method, List.copyOf(stmts));
        }
    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
        Collection<Stmt> genStmts = method2GenStmts.get(csMethod.getMethod());
        if (genStmts != null) {
            solver.addStmts(csMethod, genStmts);
        }
    }
}
