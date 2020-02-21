package sa.pta.analysis.heap;

import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Type;

public interface HeapModel {

    Obj getObj(Object allocationSite, Type type, Method containerMethod);
}
