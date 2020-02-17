package sa.pta.analysis.data;

import sa.pta.set.PointsToSet;

abstract class AbstractPointer implements Pointer {

    protected PointsToSet pointsToSet;

    @Override
    public PointsToSet getPointsToSet() {
        return pointsToSet;
    }
}
