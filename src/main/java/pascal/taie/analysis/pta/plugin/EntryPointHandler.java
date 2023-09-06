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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.World;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.JMethod;

/**
 * Initializes standard entry points for pointer analysis.
 */
public class EntryPointHandler implements Plugin {

    private Solver solver;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onStart() {
        // process program main method
        JMethod main = World.get().getMainMethod();
        if (main != null) {
            solver.addEntryPoint(new EntryPoint(main,
                    new DeclaredParamProvider(main, solver.getHeapModel(), 1)));
        }
        // process implicit entries
        if (solver.getOptions().getBoolean("implicit-entries")) {
            for (JMethod entry : World.get().getImplicitEntries()) {
                solver.addEntryPoint(new EntryPoint(entry, EmptyParamProvider.get()));
            }
        }
    }
}
