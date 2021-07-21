package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;

/**
 * Represents call edge from invokedynamic to actual target method.
 */
class InvokeDynamicCallEdge extends Edge<CSCallSite, CSMethod> {

    public InvokeDynamicCallEdge(CSCallSite csCallSite, CSMethod callee) {
        super(CallKind.OTHER, csCallSite, callee);
    }
}
