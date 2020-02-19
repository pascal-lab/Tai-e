package sa.pta.set;

import sa.pta.analysis.data.CSObj;

public interface PointsToSet extends Iterable<CSObj> {

    boolean addObject(CSObj obj);

    boolean isEmpty();
}
