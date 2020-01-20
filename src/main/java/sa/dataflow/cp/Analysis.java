package sa.dataflow.cp;

import sa.dataflow.analysis.DataFlowAnalysis;
import sa.dataflow.analysis.Meeter;
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

import java.util.stream.Stream;

public class Analysis implements DataFlowAnalysis<FlowMap, Unit> {

    private Meeter<Value> meeter = new ValueMeeter();

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public FlowMap getEntryInitialValue(Unit entry) {
        FlowMap in = newInitialValue();
        FlowMap out = newInitialValue();
        transfer(in, entry, out);
        return out;
    }

    @Override
    public FlowMap newInitialValue() {
        return new FlowMap();
    }

    @Override
    public FlowMap meet(FlowMap m1, FlowMap m2) {
        FlowMap result = newInitialValue();
        Stream.concat(m1.keySet().stream(), m2.keySet().stream())
                .distinct()
                .forEach(k -> {
                    result.put(k, meeter.meet(m1.get(k), m2.get(k)));
                });
        return result;
    }

    @Override
    public boolean transfer(FlowMap in, Unit node, FlowMap out) {
        Local lhs = null;
        if (node instanceof DefinitionStmt) {
            lhs = (Local) ((DefinitionStmt) node).getLeftOp();
        }
        boolean changed = false;
        for (Local inLocal : in.keySet()) {
            if (!inLocal.equals(lhs)) {
                changed |= out.put(inLocal, in.get(inLocal));
            }
        }
        if (lhs != null) {
            changed |= out.put(lhs, computeValue(in, lhs, node));
        }
        return changed;
    }

    private Value computeValue(FlowMap in, Local lhs, Unit node) {
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
                // Non-supported expressions
//                throw new UnsupportedOperationException(rhs + " is not a supported");
                return Value.getNAC();
            }
        } else {
            throw new IllegalArgumentException(node + " is not a definition statement");
        }
    }

    private Value toValue(FlowMap in, Type type, soot.Value v) {
        if (v instanceof Local) {
            return in.get((Local) v);
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

    private Value toValue(FlowMap in, Type type, BinopExpr expr) {
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
}
