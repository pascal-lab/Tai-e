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

import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.types.ClassType;

import java.util.Collection;

public interface ThrowAnalysis {

    Result analyze(IR ir);

    interface Result {

        IR getIR();

        Collection<ClassType> mayThrowImplicitly(Stmt stmt);

        Collection<ClassType> mayThrowExplicitly(Throw throwStmt);

        Collection<ClassType> mayThrowExplicitly(Invoke invoke);
    }
}
