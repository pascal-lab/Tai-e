/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta.plugin;

import pascal.taie.World;
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
        referenceInitThis = World.getClassHierarchy()
                .getJREMethod(REFERENCE_INIT)
                .getIR().getThis();
        referencePending = World.getClassHierarchy()
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
