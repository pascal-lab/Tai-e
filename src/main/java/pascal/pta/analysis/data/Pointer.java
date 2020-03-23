package pascal.pta.analysis.data;

import pascal.pta.set.PointsToSet;

public interface Pointer {

    void setPointsToSet(PointsToSet pointsToSet);

    PointsToSet getPointsToSet();
}
