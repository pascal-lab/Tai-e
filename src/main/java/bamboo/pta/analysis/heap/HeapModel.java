package bamboo.pta.analysis.heap;

import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;

public interface HeapModel {

    Obj getObj(Object allocationSite, Type type, Method containerMethod);
}
