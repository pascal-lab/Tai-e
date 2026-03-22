package pascal.taie.analysis.pta.plugin.cutshortcut.field;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.plugin.cutshortcut.ReflectiveEdgeProperty;
import pascal.taie.analysis.pta.plugin.reflection.ReflectiveCallEdge;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public record ParameterIndex(boolean isThis, int index) {
    public static ParameterIndex THISINDEX = new ParameterIndex(true, 0);

    public static Map<Integer, ParameterIndex> realParameters = Maps.newMap();

    @Override
    public String toString() {
        return isThis ? "%this" : index + "@parameter";
    }

    public static ParameterIndex getRealParameterIndex(int index) {
        return realParameters.computeIfAbsent(index, i -> new ParameterIndex(false, index));
    }

    public static int GetReturnVariableIndex(Var var) { // -1: 不是return Variable, 0, ... : index
        // TODO: check definitions of return variables
        if (var.getMethod().isAbstract())
            return -1;
        IR methodIR = var.getMethod().getIR();
        List<Var> returnVars = methodIR.getReturnVars();
        int len = returnVars.size(), i;
        for (i = 0; i < len; i ++)
            if (returnVars.get(i).equals(var))
                return i;
        return -1;
    }

    @Nullable
    public static Var getCorrespondingArgument(Edge<CSCallSite, CSMethod> edge, ParameterIndex parameterIndex) {
        Invoke invoke = edge.getCallSite().getCallSite();
        InvokeExp invokeExp = invoke.getInvokeExp();
        if (edge instanceof ReflectiveCallEdge reflEdge) {
            if (parameterIndex.isThis()) {
                switch (ReflectiveEdgeProperty.getReflectiveKind(reflEdge)) {
                    case NEW_INSTANCE -> { return invoke.getResult(); }
                    case METHOD_INVOKE -> { return invokeExp.getArg(0); }
                    default -> throw new AnalysisException("Invalid Reflective Call Edge");
                }
            }
            else
                return ReflectiveEdgeProperty.getVirtualArg(reflEdge);
        }
        if (!parameterIndex.isThis())
            return invokeExp.getArg(parameterIndex.index());
        else if (invokeExp instanceof InvokeInstanceExp instanceExp)
            return instanceExp.getBase();

        return null;
    }
}
