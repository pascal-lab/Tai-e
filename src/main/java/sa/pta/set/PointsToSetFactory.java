package sa.pta.set;

import sa.pta.analysis.data.CSObj;

public interface PointsToSetFactory {

    PointsToSet makePointsToSet();

    /**
     * Make a singleton points-to set.
     */
    PointsToSet makePointsToSet(CSObj obj);
}
