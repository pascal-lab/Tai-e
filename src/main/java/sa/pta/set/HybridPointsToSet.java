package sa.pta.set;

import sa.util.HybridArrayHashSet;

public class HybridPointsToSet extends DelegatePointsToSet {

    @Override
    protected void initializePointsToSet() {
        set = new HybridArrayHashSet<>();
    }

    public static class Factory implements PointsToSetFactory {

        @Override
        public PointsToSet makePointsToSet() {
            return new HybridPointsToSet();
        }
    }
}
