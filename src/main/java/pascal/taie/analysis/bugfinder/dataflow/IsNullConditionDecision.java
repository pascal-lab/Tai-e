package pascal.taie.analysis.bugfinder.dataflow;

import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.If;
import pascal.taie.language.type.ReferenceType;

import javax.annotation.CheckForNull;

public class IsNullConditionDecision {

    If conditionStmt;

    Var varTested;

    IsNullValue ifTrueDecision;

    IsNullValue ifFalseDecision;

    public IsNullConditionDecision(If stmt, Var varTested, IsNullValue ifTrueDecision, IsNullValue ifFalseDecision) {
        assert varTested.getType() instanceof ReferenceType;
        assert !(ifTrueDecision == null && ifFalseDecision == null);

        conditionStmt = stmt;
        this.varTested = varTested;
        this.ifTrueDecision = ifTrueDecision;
        this.ifFalseDecision = ifFalseDecision;
    }

    public If getConditionStmt() {
        return conditionStmt;
    }

    public Var getVarTested() {
        return varTested;
    }

    public boolean isEdgeFeasible(Edge.Kind edgeKind) {
        return getDecision(edgeKind) != null;
    }

    public @CheckForNull
    IsNullValue getDecision(Edge.Kind edgeKind) {
        if (edgeKind == Edge.Kind.IF_TRUE) {
            return ifTrueDecision;
        } else if (edgeKind == Edge.Kind.IF_FALSE) {
            return ifFalseDecision;
        } else {
            throw new UnsupportedOperationException("Incorrect edge kind: " + edgeKind);
        }
    }
}
