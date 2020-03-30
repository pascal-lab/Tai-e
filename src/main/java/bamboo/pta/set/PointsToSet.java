package bamboo.pta.set;

import bamboo.pta.analysis.data.CSObj;

import java.util.stream.Stream;

public interface PointsToSet extends Iterable<CSObj> {

    boolean addObject(CSObj obj);

    boolean isEmpty();

    Stream<CSObj> stream();
}
