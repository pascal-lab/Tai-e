package pascal.dataflow.analysis.constprop;

import pascal.dataflow.analysis.DataFlowAnalysis;
import pascal.dataflow.lattice.DataFlowTag;
import pascal.dataflow.solver.Solver;
import pascal.dataflow.solver.SolverFactory;
import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.IntType;
import soot.Local;
import soot.Type;
import soot.Unit;
import soot.jimple.AddExpr;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.SubExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Map;
import java.util.stream.Stream;

public class ConstantPropagation extends BodyTransformer
        implements DataFlowAnalysis<FlowMap, Unit> {

    private static final ConstantPropagation INSTANCE = new ConstantPropagation();

    public static ConstantPropagation v() {
        return INSTANCE;
    }

    private ConstantPropagation() {
    }

    // ---------- Data-flow analysis for constant propagation ----------
    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public FlowMap getEntryInitialFlow(Unit entry) {
        return newInitialFlow();
    }

    @Override
    public FlowMap newInitialFlow() {
        return new FlowMap();
    }

    @Override
    public FlowMap meet(FlowMap m1, FlowMap m2) {
        FlowMap result = newInitialFlow();
        Stream.concat(m1.keySet().stream(), m2.keySet().stream())
                .distinct()
                .forEach(k -> result.put(k, meetValue(m1.get(k), m2.get(k))));
        return result;
    }

    @Override
    public boolean transfer(Unit node, FlowMap in, FlowMap out) {
        Local lhs = null;
        if (node instanceof DefinitionStmt) {
            lhs = (Local) ((DefinitionStmt) node).getLeftOp();
        }
        boolean changed = false;
        for (Local inLocal : in.keySet()) {
            if (!inLocal.equals(lhs)) {
                changed |= out.update(inLocal, in.get(inLocal));
            }
        }
        if (lhs != null) {
            changed |= out.update(lhs, computeValue(node, in, lhs));
        }
        return changed;
    }

    /**
     * Computes value for a LHS variable at statement: lhs = ...
     * @param node the given statement
     * @param in in flow of the statement
     * @param lhs the LHS variable
     * @return the value of the LHS variable
     */
    public Value computeValue(Unit node, FlowMap in, Local lhs) {
        if (node instanceof IdentityStmt) {
            // the value from one of {parameters, this, caughtexception}
            return Value.getNAC();
        } else if (node instanceof AssignStmt) {
            Type type = lhs.getType();
            soot.Value rhs = ((AssignStmt) node).getRightOp();
            if (rhs instanceof Local || rhs instanceof Constant) {
                return toValue(in, type, rhs);
            } else if (rhs instanceof BinopExpr) {
                return toValue(in, type, (BinopExpr) rhs);
            } else {
                // Returns NAC for other non-supported expressions
//                throw new UnsupportedOperationException(rhs + " is not a supported");
                return Value.getNAC();
            }
        } else {
            throw new IllegalArgumentException(node + " is not a definition statement");
        }
    }

    /**
     * Evaluates a soot.Value (Local or Constant) to a Value
     * @param in FlowMap at specific program point
     * @param type type of the value
     * @param v the soot.Value to be evaluated
     * @return the resulting Value
     */
    public Value toValue(FlowMap in, Type type, soot.Value v) {
        if (v instanceof Local) {
            return in.get(v);
        } else if (v instanceof Constant) {
            if (v instanceof IntConstant) {
                int value = ((IntConstant) v).value;
                if (type.equals(IntType.v())) {
                    return Value.makeInt(value);
                } else if (type.equals(BooleanType.v())) {
                    boolean b = value != 0;
                    return Value.makeBool(b);
                }
            }
        }
        throw new UnsupportedOperationException(v + " is not a variable or boolean/integer constant");
    }

    /**
     * Evaluates a binary expression to a Value
     * @param in FlowMap at specific program point
     * @param type type of the Value
     * @param expr the expression to be evaluated
     * @return the resulting Value
     */
    public Value toValue(FlowMap in, Type type, BinopExpr expr) {
        Value op1 = toValue(in, type, expr.getOp1());
        Value op2 = toValue(in, type, expr.getOp2());
        if (op1.isInt() && op2.isInt()) {
            int i1 = op1.getInt();
            int i2 = op2.getInt();
            if (expr instanceof AddExpr) {
                return Value.makeInt(i1 + i2);
            } else if (expr instanceof SubExpr) {
                return Value.makeInt(i1 - i2);
            } else if (expr instanceof MulExpr) {
                return Value.makeInt(i1 * i2);
            } else if (expr instanceof DivExpr) {
                return Value.makeInt(i1 / i2);
            } else if (expr instanceof EqExpr) {
                return Value.makeBool(i1 == i2);
            } else if (expr instanceof NeExpr) {
                return Value.makeBool(i1 != i2);
            } else if (expr instanceof GeExpr) {
                return Value.makeBool(i1 >= i2);
            } else if (expr instanceof GtExpr) {
                return Value.makeBool(i1 > i2);
            } else if (expr instanceof LeExpr) {
                return Value.makeBool(i1 <= i2);
            } else if (expr instanceof LtExpr) {
                return Value.makeBool(i1 < i2);
            } else {
                throw new UnsupportedOperationException(expr + " is not supported");
            }
        } else if (op1.isNAC() || op2.isNAC()) {
            return Value.getNAC();
        } else {
            return Value.getUndef();
        }
    }

    /**
     * Meets two Values.
     */
    Value meetValue(Value v1, Value v2) {
        if (v1.isUndef() && v2.isConstant()) {
            return v2;
        } else if (v1.isConstant() && v2.isUndef()) {
            return v1;
        } else if (v1.isNAC() || v2.isNAC()) {
            return Value.getNAC();
        } else if (v1.equals(v2)) {
            return v1;
        } else {
            return Value.getNAC();
        }
    }

    // ---------- Body transformer ----------
    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Solver<FlowMap, Unit> solver = SolverFactory.v().newSolver(this, cfg);
        solver.solve();
        b.addTag(new DataFlowTag<>("ConstantTag", solver.getAfterFlow()));
        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(b, solver.getAfterFlow());
        } else {
            outputResult(b, solver.getAfterFlow());
        }
    }

    synchronized void outputResult(Body body, Map<Unit, FlowMap> result) {
        System.out.println("------ " + body.getMethod() + " -----");
        body.getUnits().forEach(u ->
                System.out.println("L" + u.getJavaSourceStartLineNumber()
                        + "{" + u + "}"
                        + ": " + result.get(u)));
    }
}
