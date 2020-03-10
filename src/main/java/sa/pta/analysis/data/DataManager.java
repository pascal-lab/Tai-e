package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.CallSite;
import sa.pta.element.Field;
import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Variable;
import sa.pta.set.PointsToSetFactory;

import java.util.stream.Stream;

/**
 * Manages the data structures in context-sensitive pointer analysis.
 */
public interface DataManager {

    void setPointsToSetFactory(PointsToSetFactory setFactory);

    CSVariable getCSVariable(Context context, Variable var);

    InstanceField getInstanceField(CSObj base, Field field);

    ArrayField getArrayField(CSObj array);
    
    StaticField getStaticField(Field field);

    CSObj getCSObj(Context heapContext, Obj obj);

    CSCallSite getCSCallSite(Context context, CallSite callSite);

    CSMethod getCSMethod(Context context, Method method);

    Stream<CSVariable> getCSVariables();

    Stream<InstanceField> getInstanceFields();
}
