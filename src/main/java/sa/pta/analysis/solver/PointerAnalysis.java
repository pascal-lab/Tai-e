package sa.pta.analysis.solver;

import sa.pta.analysis.ProgramManager;
import sa.pta.analysis.context.ContextSelector;
import sa.pta.analysis.heap.HeapModel;
import sa.pta.set.PointsToSetFactory;

public interface PointerAnalysis {

    ProgramManager getProgramManager();

    ContextSelector getContextSelector();

    HeapModel getHeapModel();

    PointsToSetFactory getPointsToSetFactory();
}
