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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Set;

/**
 * This class is for debugging/testing purpose.
 * <p>
 * {@link pascal.taie.analysis.pta.core.solver.Solver} needs to satisfy
 * some important constraints:
 * <ol>
 *     <li>{@code onNewMethod(m)} must happen before {@code onNewCSMethod(csM)}
 *     for any context-sensitive method {@code csM} for m.</li>
 *     <li>{@code onNewMethod(m)} must happen before {@code onNewPointsToSet(csV, pts)}
 *     for any context-sensitive variable {@code csV} in {@code m}, and</li>
 *     <li>{@code onNewCSMethod(csM)} must happen before {@code onNewPointsToSet(csV, pts)}
 *     for any context-sensitive variable {@code csV} in {@code csM}.</li>
 * </ol>
 *
 * <p>This class checks the constraints and issues warnings when they are unsatisfied.
 */
public class ConstraintChecker implements Plugin {

    private static final Logger logger = LogManager.getLogger(ConstraintChecker.class);

    private final Set<JMethod> reached = Sets.newSet(4096);

    private final Set<CSMethod> reachedCS = Sets.newSet(8192);

    private CSManager csManager;

    @Override
    public void setSolver(Solver solver) {
        csManager = solver.getCSManager();
    }

    @Override
    public void onNewMethod(JMethod method) {
        reached.add(method);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        if (!reached.contains(csMethod.getMethod())) {
            logger.warn("Warning: hit {} before processing {}",
                    csMethod, csMethod.getMethod());
        }
        reachedCS.add(csMethod);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();
        JMethod method = var.getMethod();
        if (!reached.contains(method)) {
            logger.warn("Warning: hit {} before processing {}", var, method);
        }
        CSMethod csMethod = csManager.getCSMethod(csVar.getContext(), method);
        if (!reachedCS.contains(csMethod)) {
            logger.warn("Warning: hit {} before processing {}", csVar, csMethod);
        }
    }
}
