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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.TypeSystem;

/**
 * Base class for the objects that holds a {@link Solver}.
 * It also stores various objects obtained from the {@link Solver},
 * so that its subclasses can directly access these objects.
 */
public abstract class SolverHolder {

    protected final Solver solver;

    protected final ClassHierarchy hierarchy;

    protected final TypeSystem typeSystem;

    protected final ContextSelector selector;

    protected final Context emptyContext;

    protected final CSManager csManager;

    protected final HeapModel heapModel;

    protected SolverHolder(Solver solver) {
        this.solver = solver;
        hierarchy = solver.getHierarchy();
        typeSystem = solver.getTypeSystem();
        selector = solver.getContextSelector();
        emptyContext = selector.getEmptyContext();
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
    }
}
