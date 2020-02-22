package sa.pta.set;

import sa.pta.analysis.data.CSObj;
import sa.util.HybridArrayHashSet;

import java.util.Iterator;
import java.util.Set;

public class HybridPointsToSet implements PointsToSet {

    private Set<CSObj> set = new HybridArrayHashSet<>();

    @Override
    public boolean addObject(CSObj obj) {
        return set.add(obj);
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public Iterator<CSObj> iterator() {
        return set.iterator();
    }

    public static class Factory implements PointsToSetFactory {

        @Override
        public PointsToSet makePointsToSet() {
            return new HybridPointsToSet();
        }
    }
}
