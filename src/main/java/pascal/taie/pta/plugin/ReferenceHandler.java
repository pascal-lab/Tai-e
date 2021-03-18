/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.plugin;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JField;
import pascal.taie.pta.core.cs.CSVar;
import pascal.taie.pta.core.solver.PointerAnalysis;
import pascal.taie.pta.set.PointsToSet;

import static pascal.taie.language.classes.StringReps.REFERENCE_INIT;
import static pascal.taie.language.classes.StringReps.REFERENCE_PENDING;

/**
 * Model GC behavior that it assigns every reference to Reference.pending.
 * As a result, Reference.pending can point to every reference.
 * The ReferenceHandler takes care of enqueueing the references in a
 * reference queue. If we do not model this GC behavior, Reference.pending
 * points to nothing, and finalize() methods won't get invoked.
 */
public class ReferenceHandler implements Plugin {

    private PointerAnalysis pta;

    /**
     * This variable of Reference.<init>.
     */
    private Var referenceInitThis;

    /**
     * The static field Reference.pending.
     */
    private JField referencePending;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        ClassHierarchy hierarchy = pta.getHierarchy();
        referenceInitThis = hierarchy.getJREMethod(REFERENCE_INIT)
                .getIR().getThis();
        referencePending = hierarchy.getJREField(REFERENCE_PENDING);
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        // Let Reference.pending points to every reference.
        if (csVar.getVar().equals(referenceInitThis)) {
            pta.addStaticFieldPointsTo(referencePending, pts);
        }
    }
}
