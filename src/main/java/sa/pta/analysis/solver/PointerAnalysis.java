package sa.pta.analysis.solver;

import sa.callgraph.CallGraph;
import sa.pta.analysis.ProgramManager;
import sa.pta.analysis.context.ContextSelector;
import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.DataManager;
import sa.pta.analysis.heap.HeapModel;
import sa.pta.set.PointsToSetFactory;

public interface PointerAnalysis {

    void setProgramManager(ProgramManager programManager);

    void setDataManager(DataManager dataManager);

    void setContextSelector(ContextSelector contextSelector);

    void setHeapModel(HeapModel heapModel);

    void setPointsToSetFactory(PointsToSetFactory setFactory);

    void solve();

    CallGraph<CSCallSite, CSMethod> getCallGraph();
}
