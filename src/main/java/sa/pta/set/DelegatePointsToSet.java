package sa.pta.set;

import sa.pta.analysis.data.CSObj;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Delegate points-to set to a concrete set implementation.
 */
abstract class DelegatePointsToSet implements PointsToSet {

    protected Set<CSObj> set;

    protected DelegatePointsToSet() {
        initializePointsToSet();
    }

    protected abstract void initializePointsToSet();

    @Override
    public boolean addObject(CSObj obj) {
        return set.add(obj);
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public Stream<CSObj> stream() {
        return set.stream();
    }

    @Override
    public Iterator<CSObj> iterator() {
        return set.iterator();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
