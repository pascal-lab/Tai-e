package bamboo.pta.analysis.data;

import bamboo.pta.analysis.context.Context;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Field;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Variable;
import bamboo.pta.set.PointsToSetFactory;

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
