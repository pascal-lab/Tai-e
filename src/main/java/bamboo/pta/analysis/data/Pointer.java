package bamboo.pta.analysis.data;

import bamboo.pta.set.PointsToSet;

public interface Pointer {

    void setPointsToSet(PointsToSet pointsToSet);

    PointsToSet getPointsToSet();
}
