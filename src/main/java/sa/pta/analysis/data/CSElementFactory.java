package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.CallSite;
import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Variable;

public interface CSElementFactory {

    CSVariable getCSVariable(Context context, Variable var);

    CSObj getCSObj(Context context, Obj obj);

    CSCallSite getCSCallSite(Context context, CallSite callSite);

    CSMethod getCSMethod(Context context, Method method);
}
