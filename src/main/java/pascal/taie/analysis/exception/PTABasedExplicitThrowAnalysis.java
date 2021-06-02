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

package pascal.taie.analysis.exception;

import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Stmt;

import java.util.Collection;

/**
 * Analyzes explicit exceptions based on pointer analysis.
 */
class PTABasedExplicitThrowAnalysis implements ExplicitThrowAnalysis {

    private PTABasedThrowResult ptaBasedThrowResult;

    PTABasedExplicitThrowAnalysis(){
        PointerAnalysisResult solver= World.getResult(PointerAnalysis.ID);
        this.ptaBasedThrowResult= solver.getPTABasedThrowResult();
    }

    @Override
    public void analyze(IR ir, ThrowResult result) {
        throw new UnsupportedOperationException();
    }

    public Collection<Obj> mayThrowExplicitly(IR ir){
        return ptaBasedThrowResult.mayThrowExplicitly(ir);
    }

    public Collection<Obj> mayThrowExplicitly(IR ir, Stmt stmt){
        return ptaBasedThrowResult.mayThrowExplicitly(ir,stmt);
    }
}
