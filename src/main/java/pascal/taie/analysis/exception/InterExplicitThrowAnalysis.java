package pascal.taie.analysis.exception;

import pascal.taie.ir.IR;

/**
 * Analysis explicit exceptions based on interprocedural analysis.
 * This analysis requires pointer analysis result.
 */
class InterExplicitThrowAnalysis implements ExplicitThrowAnalysis {

    @Override
    public void analyze(IR ir, ThrowResult result) {
        throw new UnsupportedOperationException();
    }
}
