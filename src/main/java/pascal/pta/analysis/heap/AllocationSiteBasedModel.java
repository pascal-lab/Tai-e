package pascal.pta.analysis.heap;

import pascal.pta.element.Method;
import pascal.pta.element.Obj;
import pascal.pta.element.Type;

import java.util.HashMap;
import java.util.Map;

public class AllocationSiteBasedModel implements HeapModel {

    private Map<Object, Obj> objects = new HashMap<>();

    @Override
    public Obj getObj(Object allocationSite, Type type, Method containerMethod) {
        return objects.computeIfAbsent(allocationSite,
                (k) -> new ObjImpl(allocationSite, type, containerMethod));
    }
}
