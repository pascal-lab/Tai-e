package pascal.taie.analysis.exception;

import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.type.ClassType;

import java.util.Collection;
import java.util.Collections;

/**
 * Dummy throw analysis which always return empty exceptions.
 */
public enum DummyThrowAnalysis implements ThrowAnalysis {

    INSTANCE;

    ThrowAnalysis get() {
        return INSTANCE;
    }

    @Override
    public Result analyze(IR ir) {

        return new Result() {
            @Override
            public IR getIR() {
                return ir;
            }

            @Override
            public Collection<ClassType> mayThrowImplicitly(Stmt stmt) {
                return Collections.emptySet();
            }

            @Override
            public Collection<ClassType> mayThrowExplicitly(Throw throwStmt) {
                return Collections.emptySet();
            }

            @Override
            public Collection<ClassType> mayThrowExplicitly(Invoke invoke) {
                return Collections.emptySet();
            }
        };
    }
}
