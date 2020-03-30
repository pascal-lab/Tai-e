package bamboo.pta.set;

import bamboo.pta.analysis.data.CSObj;

public interface PointsToSetFactory {

    PointsToSet makePointsToSet();

    /**
     * Make a singleton points-to set.
     */
    default PointsToSet makePointsToSet(CSObj obj) {
        PointsToSet set = makePointsToSet();
        set.addObject(obj);
        return set;
    }
}
