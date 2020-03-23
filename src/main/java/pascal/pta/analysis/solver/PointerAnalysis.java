package pascal.pta.analysis.solver;

import pascal.callgraph.CallGraph;
import pascal.pta.analysis.ProgramManager;
import pascal.pta.analysis.context.ContextSelector;
import pascal.pta.analysis.data.CSCallSite;
import pascal.pta.analysis.data.CSMethod;
import pascal.pta.analysis.data.CSVariable;
import pascal.pta.analysis.data.DataManager;
import pascal.pta.analysis.data.InstanceField;
import pascal.pta.analysis.heap.HeapModel;
import pascal.pta.set.PointsToSetFactory;

import java.util.stream.Stream;

public interface PointerAnalysis {

    void setProgramManager(ProgramManager programManager);

    void setDataManager(DataManager dataManager);

    void setContextSelector(ContextSelector contextSelector);

    void setHeapModel(HeapModel heapModel);

    void setPointsToSetFactory(PointsToSetFactory setFactory);

    void solve();

    CallGraph<CSCallSite, CSMethod> getCallGraph();

    /**
     *
     * @return all variables in the (reachable) program.
     */
    Stream<CSVariable> getVariables();

    /**
     *
     * @return all instance fields in the (reachable) program.
     */
    Stream<InstanceField> getInstanceFields();
}
