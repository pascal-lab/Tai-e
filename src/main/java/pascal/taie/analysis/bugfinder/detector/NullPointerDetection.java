package pascal.taie.analysis.bugfinder.detector;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.bugfinder.BugInstance;
import pascal.taie.analysis.bugfinder.Severity;
import pascal.taie.analysis.bugfinder.dataflow.IsNullAnalysis;
import pascal.taie.analysis.bugfinder.dataflow.IsNullConditionDecision;
import pascal.taie.analysis.bugfinder.dataflow.IsNullFact;
import pascal.taie.analysis.bugfinder.dataflow.IsNullValue;
import pascal.taie.analysis.dataflow.fact.NodeResult;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class NullPointerDetection extends MethodAnalysis<Set<BugInstance>> {

    public static String ID = "nullpointer";

    public NullPointerDetection(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<BugInstance> analyze(IR ir) {
        NodeResult<Stmt, IsNullFact> nullValues = ir.getResult(IsNullAnalysis.ID);
        Set<BugInstance> bugInstances = Sets.newSet();

        bugInstances.addAll(findNullDeref(ir, nullValues));
        bugInstances.addAll(findRedundantComparison(ir, nullValues));

        return bugInstances;
    }

    private Set<BugInstance> findNullDeref(IR ir, NodeResult<Stmt, IsNullFact> nullValues) {
        Set<BugInstance> nullDerefs = Sets.newSet();
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        for (Stmt stmt : cfg.getNodes()) {
            Var derefVar = stmt.accept(new IsNullAnalysis.NPEVarVisitor());
            if (derefVar != null) {
                IsNullFact prevFact = null;
                for(Edge<Stmt> inEdge: cfg.getInEdgesOf(stmt)){
                    if(inEdge.getKind() == Edge.Kind.FALL_THROUGH){
                        prevFact = nullValues.getOutFact(inEdge.getSource());
                    }
                }

                if(prevFact != null && prevFact.isValid()){
                    IsNullValue derefVarValue = prevFact.get(derefVar);
                    if(derefVarValue.isDefinitelyNull()){
                        nullDerefs.add(
                            BugInstance.newBugInstance("NP_ALWAYS_NULL", Severity.BLOCKER, ir.getMethod(), stmt.getLineNumber())
                        );
                    } else if(derefVarValue.isNullOnSomePath()){
                        nullDerefs.add(
                            BugInstance.newBugInstance("NP_MAY_NULL", Severity.CRITICAL, ir.getMethod(), stmt.getLineNumber())
                        );
                    }
                }
            }
        }

        return nullDerefs;
    }

    private Set<BugInstance> findRedundantComparison(IR ir, NodeResult<Stmt, IsNullFact> nullValues) {
        Set<BugInstance> redundantComparisons = Sets.newSet();

        for (Stmt stmt : ir.getStmts()) {
            if (stmt instanceof If ifStmt) {
                IsNullFact fact = nullValues.getOutFact(stmt);
                if(!fact.isValid()) {
                    continue;
                }
                IsNullConditionDecision decision = fact.getDecision();

                if (decision != null) {
                    Var varTested = decision.getVarTested();
                    IsNullValue varTestedValue = fact.get(varTested);
                    Var var1 = ifStmt.getCondition().getOperand1();
                    Var var2 = ifStmt.getCondition().getOperand2();
                    String bugType = null;

                    Var anotherVar = varTested == var1 ? var2 : var1;
                    if (anotherVar.getType() instanceof NullType) {
                        if (varTestedValue.isAKaBoom()) {
                            bugType = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE";
                        } else if (varTestedValue.isDefinitelyNotNull()) {
                            bugType = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE";
                        } else if (varTestedValue.isDefinitelyNull()) {
                            bugType = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE";
                        }
                    } else {
                        IsNullValue anotherVarValue = fact.get(anotherVar);
                        if (varTestedValue.isDefinitelyNull()) {
                            if (anotherVarValue.isDefinitelyNull()) {
                                bugType = "RCN_REDUNDANT_COMPARISON_TWO_NULL_VALUES";
                            } else if (anotherVarValue.isDefinitelyNotNull()) {
                                bugType = "RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE";
                            }
                        } else if (varTestedValue.isDefinitelyNotNull() && anotherVarValue.isDefinitelyNull()) {
                            bugType = "RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE";
                        }
                    }

                    if (bugType != null) {
                        redundantComparisons.add(
                            BugInstance.newBugInstance(bugType, Severity.MAJOR, ir.getMethod(), stmt.getLineNumber())
                        );
                    }
                }
            }
        }

        return redundantComparisons;
    }
}
