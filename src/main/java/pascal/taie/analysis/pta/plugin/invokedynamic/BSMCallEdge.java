package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.language.classes.JClass;

public class BSMCallEdge  extends Edge<CSCallSite, CSMethod> {

    private JClass lookupClass;

    public BSMCallEdge(CSCallSite csCallSite, CSMethod callee, JClass lookupClass) {
        super(CallKind.OTHER, csCallSite, callee);
        this.lookupClass = lookupClass;
    }

    public JClass getLookupClass() {
        return lookupClass;
    }

    public void setLookupClass(JClass lookupClass) {
        this.lookupClass = lookupClass;
    }
}
