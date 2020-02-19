package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.CallSite;
import sa.pta.element.Field;
import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Variable;
import sa.pta.set.PointsToSetFactory;

public interface ElementManager {

    void setPointsToSetFactory(PointsToSetFactory setFactory);

    CSVariable getCSVariable(Context context, Variable var);

    InstanceField getInstanceField(CSObj base, Field field);

    StaticField getStaticField(Field field);

    CSObj getCSObj(Context heapContext, Obj obj);

    CSCallSite getCSCallSite(Context context, CallSite callSite);

    CSMethod getCSMethod(Context context, Method method);
}
