package bamboo.pta.analysis.data;

import bamboo.pta.set.PointsToSet;

abstract class AbstractPointer implements Pointer {

    protected PointsToSet pointsToSet;

    @Override
    public void setPointsToSet(PointsToSet pointsToSet) {
        this.pointsToSet = pointsToSet;
    }

    @Override
    public PointsToSet getPointsToSet() {
        return pointsToSet;
    }
}
