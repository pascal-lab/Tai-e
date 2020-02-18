package sa.pta.analysis.solver;

import sa.callgraph.CallGraph;
import sa.pta.analysis.ProgramManager;
import sa.pta.analysis.context.ContextSelector;
import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.heap.HeapModel;
import sa.pta.set.PointsToSetFactory;

public interface PointerAnalysis {

    // set* or get*?

    ProgramManager getProgramManager();

    ContextSelector getContextSelector();

    HeapModel getHeapModel();

    PointsToSetFactory getPointsToSetFactory();

    CallGraph<CSCallSite, CSMethod> getCallGraph();
}
