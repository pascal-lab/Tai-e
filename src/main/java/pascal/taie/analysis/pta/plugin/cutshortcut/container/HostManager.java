package pascal.taie.analysis.pta.plugin.cutshortcut.container;

import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cutshortcut.SpecialVariables;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContExitCategory;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMultiMap;

import java.util.Map;

public class HostManager {
    private final CSManager csManager;

    // assign a host variable for each container object
    private final Map<Obj, Var> hostMap = Maps.newMap();
    // map host variable to host object
    private final Map<Var, Obj> hostVarToObj = Maps.newMap();

    // denotes [HostSource], mapping container object <c, o> to set of host variables <c', v> that container.entrance(v), where o \in pts(container)
    private final TwoKeyMultiMap<CSObj, ContExitCategory, CSVar> HostSourceMap = Maps.newTwoKeyMultiMap();

    // denotes [HostTarget], mapping container object <c, o> to set of host variables <c', v> that v = container.exit(), where o \in pts(container)
    private final TwoKeyMultiMap<CSObj, ContExitCategory, CSVar> HostTargetMap = Maps.newTwoKeyMultiMap();

    public HostManager(CSManager csManager) {
        this.csManager = csManager;
    }

    public CSVar getHostVar(CSObj containerCSObj, ContExitCategory category) {
        Obj containerObj = containerCSObj.getObject();

        Var hostVar = hostMap.computeIfAbsent(containerObj, obj -> {
            String varName = "host of[" + obj.toString() + "]," + category.getCategory();
            Var newHostVar = new Var(obj.getContainerMethod().orElse(null), varName, containerObj.getType(), -1);
            SpecialVariables.setVirtualVar(newHostVar);
            hostVarToObj.put(newHostVar, obj);
            return newHostVar;
        });
        return csManager.getCSVar(containerCSObj.getContext(), hostVar);
    }

    public boolean addHostSource(CSObj containerCSObj, ContExitCategory category, CSVar hostVar) {
        return HostSourceMap.put(containerCSObj, category, hostVar);
    }

    public boolean addHostTarget(CSObj containerCSObj, ContExitCategory category, CSVar hostVar) {
        return HostTargetMap.put(containerCSObj, category, hostVar);
    }

    public Obj getHostObj(Var var) { return hostVarToObj.getOrDefault(var, null); }
}
