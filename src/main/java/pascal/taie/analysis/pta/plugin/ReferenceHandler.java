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

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;

import static pascal.taie.language.classes.Signatures.REFERENCE_INIT;
import static pascal.taie.language.classes.Signatures.REFERENCE_PENDING;

/**
 * Models GC behavior that it assigns every reference to Reference.pending.
 * As a result, Reference.pending can point to every reference.
 * The ReferenceHandler takes care of enqueueing the references in a
 * reference queue. If we do not model this GC behavior, Reference.pending
 * points to nothing, and finalize() methods won't get invoked.
 * TODO: update it for Java 9+ (current model doesn't work since Java 9).
 */
public class ReferenceHandler implements Plugin {

    private Solver solver;

    /**
     * This variable of Reference.<init>.
     */
    private Var referenceInitThis;

    /**
     * The static field Reference.pending.
     * This field has been deprecated since Java 9.
     */
    private JField referencePending;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        referenceInitThis = solver.getHierarchy()
                .getJREMethod(REFERENCE_INIT)
                .getIR().getThis();
        referencePending = solver.getHierarchy()
                .getJREField(REFERENCE_PENDING);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        // Let Reference.pending points to every reference.
        if (csVar.getVar().equals(referenceInitThis)) {
            solver.addStaticFieldPointsTo(referencePending, pts);
        }
    }
}
