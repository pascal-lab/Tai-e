package pascal.callgraph;

import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;

public class JimpleCallUtils {

    public static CallKind getCallKind(InvokeExpr invoke) {
        if (invoke instanceof VirtualInvokeExpr ||
                invoke instanceof InterfaceInvokeExpr) {
            return CallKind.VIRTUAL;
        } else if (invoke instanceof SpecialInvokeExpr) {
            return CallKind.SPECIAL;
        } else if (invoke instanceof StaticInvokeExpr) {
            return CallKind.STATIC;
        } else {
            return CallKind.OTHER;
        }
    }
}
