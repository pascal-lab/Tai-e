package sa.pta.analysis.heap;

import sa.pta.element.Obj;

import java.util.HashMap;
import java.util.Map;

public class AllocationSiteBasedModel implements HeapModel {

    private Map<Object, Obj> objectMap = new HashMap<>();

    @Override
    public Obj getObj(Object allocationSite) {
        throw new UnsupportedOperationException();
    }
}
