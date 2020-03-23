package pascal.pta.analysis.heap;

import pascal.pta.element.Method;
import pascal.pta.element.Obj;
import pascal.pta.element.Type;

public interface HeapModel {

    Obj getObj(Object allocationSite, Type type, Method containerMethod);
}
