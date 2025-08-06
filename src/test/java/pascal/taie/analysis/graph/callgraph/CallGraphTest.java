package pascal.taie.analysis.graph.callgraph;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallGraphTest {

    @Test
    void test() {
        Main.main("-pp",
                "-cp", "src/test/resources/pta/contextsensitivity",
                "-m", "LinkedQueue",
                "-scope", "ALL",
                "-a", "pta");
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        CallGraph<CSCallSite, CSMethod> csCallGraph = pta.getCSCallGraph();
        CallGraph<Invoke, JMethod> callGraph = pta.getCallGraph();

        assertEquals(csCallGraph.getNumberOfNodes(), callGraph.getNumberOfNodes());
        assertEquals(csCallGraph.edges().count(), callGraph.getNumberOfEdges());
        assertEquals(csCallGraph.getNumberOfEdges(), csCallGraph.edges().count());
    }
}
