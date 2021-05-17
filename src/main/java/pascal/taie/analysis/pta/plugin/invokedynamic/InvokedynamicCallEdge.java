package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;

public class InvokedynamicCallEdge  extends Edge<CSCallSite, CSMethod> {

    public InvokedynamicCallEdge(CSCallSite csCallSite, CSMethod callee) {
        super(CallKind.OTHER, csCallSite, callee);
    }

}
