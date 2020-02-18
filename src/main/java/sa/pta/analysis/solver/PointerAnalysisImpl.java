package sa.pta.analysis.solver;

import sa.pta.analysis.ProgramManager;
import sa.pta.analysis.context.ContextSelector;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.ElementManager;
import sa.pta.analysis.heap.HeapModel;
import sa.pta.set.PointsToSetFactory;

public abstract class PointerAnalysisImpl implements PointerAnalysis {

    private HeapModel heapModel;

    private ContextSelector contextSelector;

    private PointsToSetFactory setFactory;

    private ProgramManager programManager;

    private ElementManager elementManager;

    @Override
    public HeapModel getHeapModel() {
        return heapModel;
    }

    @Override
    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    @Override
    public PointsToSetFactory getPointsToSetFactory() {
        return setFactory;
    }

    @Override
    public ProgramManager getProgramManager() {
        return null;
    }

    public void solve() {
        initialize();

    }

    private void initialize() {
        programManager.getEntryMethods()
                .stream()
                .map(m -> elementManager
                        .getCSMethod(contextSelector.getDefaultContext(), m))
                .forEach(this::processNewMethod);
    }

    private void processNewMethod(CSMethod csmethod) {

    }
}
