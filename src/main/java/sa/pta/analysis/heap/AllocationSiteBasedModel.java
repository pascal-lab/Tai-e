package sa.pta.analysis.heap;

import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Type;

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
