package pascal.pta.analysis.data;

import pascal.pta.analysis.context.Context;
import pascal.pta.element.CallSite;
import pascal.pta.element.Field;
import pascal.pta.element.Method;
import pascal.pta.element.Obj;
import pascal.pta.element.Variable;
import pascal.pta.set.PointsToSetFactory;

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
