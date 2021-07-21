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
import pascal.taie.analysis.pta.plugin.exception.PTAThrowResult;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.type.ClassType;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Analyzes explicit exceptions based on pointer analysis.
 */
class PTABasedExplicitThrowAnalysis implements ExplicitThrowAnalysis {

    private final PTAThrowResult ptaBasedThrowResult;

    PTABasedExplicitThrowAnalysis() {
        PointerAnalysisResult solver = World.getResult(PointerAnalysis.ID);
        this.ptaBasedThrowResult = solver.getThrowResult();
    }

    @Override
    public void analyze(IR ir, ThrowResult result) {
        ptaBasedThrowResult.getResult(ir.getMethod())
                .ifPresent(ptaResult ->
                        ir.getStmts().forEach(stmt -> {
                            Collection<ClassType> exceptions = ptaResult.mayThrow(stmt)
                                    .stream()
                                    .map(o -> (ClassType) o.getType())
                                    .collect(Collectors.toUnmodifiableSet());
                            if (!exceptions.isEmpty()) {
                                if (stmt instanceof Throw) {
                                    result.addExplicit((Throw) stmt, exceptions);
                                } else if (stmt instanceof Invoke) {
                                    result.addExplicit((Invoke) stmt, exceptions);
                                }
                            }
                        })
                );
    }
}
