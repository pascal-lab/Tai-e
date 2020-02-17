package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.CallSite;
import sa.pta.element.Field;
import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Variable;

public interface ElementManager {

    CSVariable getCSVariable(Context context, Variable var);

    InstanceField getInstanceField(CSObj base, Field field);

    StaticField getStaticField(Field field);

    CSObj getCSObj(Context context, Obj obj);

    CSCallSite getCSCallSite(Context context, CallSite callSite);

    CSMethod getCSMethod(Context context, Method method);
}
