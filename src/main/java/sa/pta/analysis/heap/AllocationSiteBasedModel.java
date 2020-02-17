package sa.pta.analysis.heap;

import sa.pta.element.Obj;

public class AllocationSiteBasedModel implements HeapModel {

    @Override
    public Obj getObject(Obj obj) {
        return obj;
    }
}
